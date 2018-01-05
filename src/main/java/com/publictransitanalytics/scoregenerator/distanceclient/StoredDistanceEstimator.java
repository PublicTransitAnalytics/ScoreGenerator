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

import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Distance estimation directory that creates an exhaustive store under a
 * maximum distance.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class StoredDistanceEstimator implements CalculatingDistanceEstimator {

    private final double maxDistanceMeters;
    private final EstimateStorage estimateStorage;

    public StoredDistanceEstimator(
            final PairGenerator pairStorer, final double maxDistanceMeters,
            final EstimateStorage estimateStorage)
            throws InterruptedException {

        this.estimateStorage = estimateStorage;
        this.maxDistanceMeters = maxDistanceMeters;

        pairStorer.storeEstimates(this, estimateStorage, maxDistanceMeters);
    }

    @Override
    public Set<PointLocation> getReachableLocations(
            final PointLocation origin, final double distanceMeters)
            throws InterruptedException {

        if (distanceMeters > maxDistanceMeters) {
            throw new ScoreGeneratorFatalException(String.format(
                    "Cannot get locations %f meters away; maximum distance is %f.",
                    distanceMeters, maxDistanceMeters));
        }
        return estimateStorage.getReachable(origin, distanceMeters);

    }

    @Override
    public void close() {
        estimateStorage.close();
    }

    @Override
    public void generateEstimate(
            final PointLocation origin, final PointLocation destination)
            throws InterruptedException {

        if (!origin.equals(destination)) {
            
            final double distanceMeters = estimateDistanceMeters(
                    origin.getLocation(), destination.getLocation());

            if (distanceMeters <= maxDistanceMeters) {
                estimateStorage.put(origin, destination, distanceMeters);
            }
        }
    }

    private static double estimateDistanceMeters(final GeoPoint a,
                                                 final GeoPoint b) {
        return a.getDistanceMeters(b);
    }
}
