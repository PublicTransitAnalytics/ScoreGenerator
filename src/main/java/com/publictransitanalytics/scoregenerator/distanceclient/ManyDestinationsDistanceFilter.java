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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Filters the estimated distances of locations to remove locations that are not
 * reachable in the times given. Treats these points as though they are
 * destinations and we have a single origin. Thus time is moving forward, as we
 * are branching out on the possibilities of what can be reached.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class ManyDestinationsDistanceFilter implements DistanceFilter {

    private final DistanceClient client;

    @Override
    public Map<VisitableLocation, WalkingCosts> getFilteredDistances(
            final PointLocation origin,
            final Set<VisitableLocation> destinations,
            final LocalDateTime currentTime, final LocalDateTime cutoffTime)
            throws DistanceClientException {

        final ImmutableSet.Builder<VisitableLocation> queryDestinationsBuilder
                = ImmutableSet.builder();
        for (final VisitableLocation destination : destinations) {
            /* If the nearest point to the origin from the destination is the 
             * origin point itself, either these points are exactly the same or
             * the point is within the sector. */
            if (!destination.getNearestPoint(origin.getLocation()).equals(origin
                    .getLocation())) {
                queryDestinationsBuilder.add(destination);
            }
        }
        final ImmutableSet<VisitableLocation> queryDestinations
                = queryDestinationsBuilder.build();

        final ImmutableMap.Builder<VisitableLocation, WalkingCosts> resultBuilder
                = ImmutableMap.builder();

        if (!queryDestinations.isEmpty()) {

            final Table<VisitableLocation, VisitableLocation, WalkingCosts> distanceTable
                    = client.getDistances(Collections.singleton(origin),
                                          queryDestinations);
            final Map<VisitableLocation, WalkingCosts> costMap
                    = distanceTable.row(origin);

            final Duration duration = Duration.between(currentTime, cutoffTime);

            for (Map.Entry<VisitableLocation, WalkingCosts> costEntry : costMap
                    .entrySet()) {
                if (costEntry.getValue().getDuration().compareTo(duration)
                            <= 0) {
                    resultBuilder.put(costEntry.getKey(), costEntry.getValue());
                }
            }
        }

        return resultBuilder.build();
    }

}
