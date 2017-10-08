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
package com.publictransitanalytics.scoregenerator.testhelpers;

import com.publictransitanalytics.scoregenerator.distanceclient.EstimateStorage;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import edu.emory.mathcs.backport.java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedEstimateStorage implements EstimateStorage {

    private final boolean initialized;
    private final double maxStored;

    @Override
    public void put(final PointLocation origin, 
                    final VisitableLocation destination,
                    final double distanceMeters) throws InterruptedException {
    }

    @Override
    public Set<VisitableLocation> getReachable(final PointLocation origin,
                                               final double distanceMeters) 
            throws InterruptedException {
        return Collections.emptySet();
    }

    @Override
    public void updateMaxStored(final PointLocation orgin, final double max) 
            throws InterruptedException {
    }

    @Override
    public void close() {
    }

    @Override
    public double getMaxStored(final PointLocation origin)
            throws InterruptedException {
        return maxStored;
    }

    @Override
    public boolean isInitialized() throws InterruptedException {
        return initialized;
    }

}
