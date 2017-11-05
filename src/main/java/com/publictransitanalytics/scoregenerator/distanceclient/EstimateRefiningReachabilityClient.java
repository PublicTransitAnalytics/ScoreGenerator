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

import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Computes a map walking costs by using an exhaustive distance estimator, and
 * then filtering the results of the estimator against a precise distance
 * finder.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class EstimateRefiningReachabilityClient implements ReachabilityClient {

    private final DistanceClient distanceClient;
    private final DistanceEstimator distanceEstimator;
    private final TimeTracker timeTracker;
    private final double walkingMetersPerSecond;

    @Override
    public Map<VisitableLocation, WalkingCosts> getWalkingDistances(
            final PointLocation location, final LocalDateTime currentTime,
            final LocalDateTime cutoffTime)
            throws DistanceClientException, InterruptedException {

        final Duration duration
                = timeTracker.getDuration(currentTime, cutoffTime);

        final double maximumDistanceMeters = duration.getSeconds()
                                                     * walkingMetersPerSecond;
        final Set<VisitableLocation> candidateLocations
                = distanceEstimator.getReachableLocations(
                        location, maximumDistanceMeters);
        if (candidateLocations == null) {
            throw new ScoreGeneratorFatalException(String.format(
                    "Distance estimates did not contain location %s",
                    location.getIdentifier()));
        }

        if (candidateLocations.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final Map<VisitableLocation, WalkingCosts> costs
                    = distanceClient.getDistances(location, candidateLocations);
            
            return costs.entrySet().stream()
                    .filter(entry -> entry.getValue().getDuration().compareTo(
                    duration) <= 0)
                    .collect(Collectors.toMap(entry -> entry.getKey(),
                                              entry -> entry.getValue()));
        }
    }
}
