/*
 * Copyright 2018 Public Transit Analytics.
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
package com.publictransitanalytics.scoregenerator.distance;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class RangedCachingReachabilityClient implements ReachabilityClient {

    private final DistanceStoreManager store;
    private final Set<PointLocation> points;
    private final TimeTracker timeTracker;
    private final DistanceClient distanceClient;
    private final DistanceClient estimationClient;

    @Override
    public Map<PointLocation, WalkingCosts> getWalkingCosts(
            final PointLocation location, final LocalDateTime currentTime,
            final LocalDateTime cutoffTime) throws DistanceClientException,
            InterruptedException {

        final Duration duration
                = timeTracker.getDuration(currentTime, cutoffTime);

        final ImmutableMap.Builder<PointLocation, WalkingCosts> builder
                = ImmutableMap.builder();
        final Set<PointLocation> uncached;
        final boolean updateMaxStored;
        final Duration maxStored = store.getMaxStored(location);
        if (maxStored != null) {
            final Map<PointLocation, WalkingCosts> cached
                    = store.get(location, duration);
            builder.putAll(cached);

            if (duration.compareTo(maxStored) > 0) {
                uncached = Sets.difference(points, cached.keySet());
                updateMaxStored = true;
            } else {
                uncached = Collections.emptySet();
                updateMaxStored = false;
            }
        } else {
            uncached = points;
            updateMaxStored = true;
        }

        if (!uncached.isEmpty()) {
            final Map<PointLocation, WalkingCosts> estimatedCosts
                    = estimationClient.getDistances(location, uncached);
            final Map<PointLocation, WalkingCosts> candidateEstimates
                    = filterTimes(estimatedCosts, duration);
            final Set<PointLocation> candidates
                    = candidateEstimates.keySet();

            if (!candidates.isEmpty()) {
                final Map<PointLocation, WalkingCosts> candidateCosts
                        = distanceClient.getDistances(location, candidates);
                final Map<PointLocation, WalkingCosts> filteredDistances
                        = filterTimes(candidateCosts, duration);
                store.putAll(location, filteredDistances);
                builder.putAll(filteredDistances);
            }
        }

        if (updateMaxStored) {
            store.updateMaxStored(location, duration);
        }
        return builder.build();

    }

    private Map<PointLocation, WalkingCosts> filterTimes(
            final Map<PointLocation, WalkingCosts> distances,
            final Duration duration) {
        return distances.entrySet().stream()
                .filter(entry -> entry.getValue().getDuration().compareTo(
                duration) <= 0)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
}
