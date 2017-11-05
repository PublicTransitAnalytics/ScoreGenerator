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
public class SupplementalPairGenerator implements PairGenerator {

    private final Set<PointLocation> existingOrigins;
    private final Set<VisitableLocation> existingDestinations;
    private final Set<PointLocation> newPoints;

    public SupplementalPairGenerator(final Set<Sector> existingSectors,
                                     final Set<TransitStop> existingStops,
                                     final Set<PointLocation> existingCenters,
                                     final Set<PointLocation> newPoints) {
        existingOrigins = ImmutableSet.<PointLocation>builder()
                .addAll(existingCenters).addAll(existingStops).build();
        existingDestinations = ImmutableSet.<VisitableLocation>builder()
                .addAll(existingSectors).addAll(existingStops).build();
        this.newPoints = newPoints;
    }

    @Override
    public void storeEstimates(final CalculatingDistanceEstimator estimator,
                               final EstimateStorage estimateStorage,
                               final double maxDistanceMeters)
            throws InterruptedException {

        for (final PointLocation newPoint : newPoints) {
            for (final VisitableLocation existingDestination
                         : existingDestinations) {
                if (!newPoint.equals(existingDestination)) {
                    estimator.generateEstimate(newPoint, existingDestination);
                }
            }
        }

        for (final PointLocation existingOrigin : existingOrigins) {
            for (final PointLocation newPoint : newPoints) {
                if (!newPoint.equals(existingOrigin)) {
                    estimator.generateEstimate(existingOrigin, newPoint);
                }
            }
        }
    }
}
