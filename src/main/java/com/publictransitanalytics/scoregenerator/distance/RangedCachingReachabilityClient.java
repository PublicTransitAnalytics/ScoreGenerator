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

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.RangedKeyStore;
import com.bitvantage.bitvantagecaching.Store;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationKey;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationTimeKey;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class RangedCachingReachabilityClient implements ReachabilityClient {

    private final RangedKeyStore<LocationTimeKey> timeStore;
    private final Store<LocationKey, Integer> maxTimeStore;
    private final TimeTracker timeTracker;
    private final DistanceClient distanceClient;
    private final DistanceClient estimationClient;
    private final BiMap<String, PointLocation> pointIdMap;

    @Override
    public Map<PointLocation, WalkingCosts> getWalkingCosts(
            final PointLocation location, final LocalDateTime currentTime,
            final LocalDateTime cutoffTime) throws DistanceClientException,
            InterruptedException {

        final Duration duration
                = timeTracker.getDuration(currentTime, cutoffTime);
        final int durationSeconds = (int) duration.getSeconds();

        final String locationId = location.getIdentifier();

        try {
            final Integer maxStored = maxTimeStore.get(new LocationKey(
                    locationId));

            final ImmutableMap.Builder<PointLocation, WalkingCosts> builder
                    = ImmutableMap.builder();
            final Set<PointLocation> uncached;
            final boolean updateMaxStored;
            if (maxStored != null) {
                final NavigableSet<LocationTimeKey> values
                        = timeStore.getValuesBelow(LocationTimeKey.getMaxKey(
                                locationId, durationSeconds));
                final Map<PointLocation, WalkingCosts> cached
                        = convertFromKeys(values);
                builder.putAll(cached);

                if (durationSeconds > maxStored) {
                    uncached = Sets.difference(pointIdMap.values(),
                                               cached.keySet());
                    updateMaxStored = true;
                } else {
                    uncached = Collections.emptySet();
                    updateMaxStored = false;
                }
            } else {
                uncached = pointIdMap.values();
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
                    timeStore.putAll(
                            convertToKeys(locationId, filteredDistances));

                    builder.putAll(filteredDistances);
                }
            }

            if (updateMaxStored) {
                maxTimeStore.put(new LocationKey(locationId), durationSeconds);
            }
            return builder.build();
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    private Map<PointLocation, WalkingCosts> convertFromKeys(
            final Set<LocationTimeKey> keys) {
        return keys.stream().collect(Collectors.toMap(
                key -> pointIdMap.get(key.getDestinationId()),
                key -> new WalkingCosts(Duration.ofSeconds(
                        key.getTimeSeconds()), -1)));
    }

    private Set<LocationTimeKey> convertToKeys(
            final String locationId,
            final Map<PointLocation, WalkingCosts> values) {
        return values.entrySet().stream().map(
                entry -> LocationTimeKey.getWriteKey(
                        locationId,
                        (int) entry.getValue().getDuration().getSeconds(),
                        entry.getKey().getIdentifier()))
                .collect(Collectors.toSet());
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
