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
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
public class CompletePairGenerator implements PairGenerator {

    private final Set<TransitStop> stops;
    private final Set<PointLocation> centers;
    private final Set<VisitableLocation> destinations;

    public CompletePairGenerator(final Set<Sector> sectors,
                                 final Set<TransitStop> stops,
                                 final Set<PointLocation> centers) {
        this.stops = stops;
        this.centers = centers;
        destinations = ImmutableSet.<VisitableLocation>builder()
                .addAll(stops).addAll(sectors).build();
    }

    @Override
    public void storeEstimates(final CalculatingDistanceEstimator estimator,
                               final EstimateStorage estimateStorage,
                               final double maxDistanceMeters)
            throws InterruptedException {

        if (!estimateStorage.isInitialized()) {
            for (final TransitStop point : stops) {
                for (final VisitableLocation destination : destinations) {
                    if (!point.equals(destination)) {
                        estimator.generateEstimate(point, destination);
                    }
                }
            }
        }

        for (final PointLocation center : centers) {
            final double maxStored = estimateStorage.getMaxStored(center);
            if (maxStored < maxDistanceMeters) {
                for (final VisitableLocation destination : destinations) {
                    if (!center.equals(destination)) {
                        estimator.generateEstimate(center, destination);
                    }
                }
            }
        }
    }

}
