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
package com.publictransitanalytics.scoregenerator.distanceclient;

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class CompletePairGenerator implements PairGenerator {

    private final Set<TransitStop> stops;
    private final Set<PointLocation> centers;
    private final Set<PointLocation> destinations;

    public CompletePairGenerator(final Set<GridPoint> gridPoints,
                                 final Set<TransitStop> stops,
                                 final Set<PointLocation> centers) {
        this.stops = stops;
        this.centers = centers;
        destinations = ImmutableSet.<PointLocation>builder()
                .addAll(stops).addAll(gridPoints).build();
    }

    @Override
    public void storeEstimates(final CalculatingDistanceEstimator estimator,
                               final EstimateStorage estimateStorage,
                               final double maxDistanceMeters)
            throws InterruptedException {

        if (!estimateStorage.isInitialized()) {
            for (final TransitStop point : stops) {
                for (final PointLocation destination : destinations) {
                    if (!point.equals(destination)) {
                        estimator.generateEstimate(point, destination);
                    }
                }
            }
        }

        for (final PointLocation center : centers) {
            final double maxStored = estimateStorage.getMaxStored(center);
            if (maxStored < maxDistanceMeters) {
                for (final PointLocation destination : destinations) {
                    if (!center.equals(destination)) {
                        estimator.generateEstimate(center, destination);
                    }
                }
                estimateStorage.updateMaxStored(center, maxDistanceMeters);
            } else {
                log.debug("Not regenerating estimates for {}", center);
            }
        }
    }

}
