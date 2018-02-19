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

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.BoundsStatus;
import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.Store;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.GeoBounds;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.BoundsKey;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class StoredInEnvironmentDetector implements InEnvironmentDetector {

    final InEnvironmentDetector waterDetector;
    final Store<BoundsKey, BoundsStatus> waterBoundsStore;

    @Override
    public boolean isOutOfBounds(final GeoPoint point)
            throws InEnvironmentDetectorException, InterruptedException {
        return waterDetector.isOutOfBounds(point);
    }

    @Override
    public boolean isOutOfBounds(final GeoBounds bounds)
            throws InEnvironmentDetectorException, InterruptedException {
        try {
            final BoundsKey key = new BoundsKey(bounds);
            final BoundsStatus status = waterBoundsStore.get(key);

            if (status == null) {
                final boolean outOfBounds = waterDetector.isOutOfBounds(bounds);
                waterBoundsStore.put(key, outOfBounds
                                     ? BoundsStatus.OUT_OF_BOUNDS
                                     : BoundsStatus.SOME_IN_BOUNDS);
                return outOfBounds;
            }
            return status.equals(BoundsStatus.OUT_OF_BOUNDS);
        } catch (BitvantageStoreException e) {
            throw new InEnvironmentDetectorException(e);
        }
    }

}
