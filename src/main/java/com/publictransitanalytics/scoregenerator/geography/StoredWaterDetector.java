/*
 * Copyright 2017 Public Transit Analytics.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.publictransitanalytics.scoregenerator.geography;

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.WaterStatus;
import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.Store;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.BoundsKey;
import lombok.RequiredArgsConstructor;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class StoredWaterDetector implements WaterDetector {

    final WaterDetector waterDetector;
    final Store<BoundsKey, WaterStatus> waterBoundsStore;

    @Override
    public boolean isOnWater(final Geodetic2DPoint point) 
            throws WaterDetectorException, InterruptedException {
        return waterDetector.isOnWater(point);
    }

    @Override
    public boolean isEntirelyWater(final Geodetic2DBounds bounds)
            throws WaterDetectorException, InterruptedException {
        try {
            final BoundsKey key = new BoundsKey(bounds);
            final WaterStatus status = waterBoundsStore.get(key);
            
            if (status == null) {
                final boolean allWater = waterDetector.isEntirelyWater(bounds);
                waterBoundsStore.put(key, allWater ? WaterStatus.ALL_WATER
                        : WaterStatus.SOME_LAND);
                return allWater;
            }
            return status.equals(WaterStatus.ALL_WATER);
        } catch (BitvantageStoreException e) {
            throw new WaterDetectorException(e);
        }
    }

}
