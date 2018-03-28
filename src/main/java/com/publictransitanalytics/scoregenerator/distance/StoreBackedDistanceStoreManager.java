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
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationKey;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationTimeKey;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class StoreBackedDistanceStoreManager implements DistanceStoreManager {

    private final RangedKeyStore<LocationTimeKey> timeStore;
    private final Store<LocationKey, Integer> maxTimeStore;
    private final BiMap<String, PointLocation> pointIdMap;

    @Override
    public Duration getMaxStored(final PointLocation location)
            throws InterruptedException {
        final String locationId = location.getIdentifier();
        try {
            final Integer maxStored = maxTimeStore.get(new LocationKey(
                    locationId));
            return (maxStored == null) ? null : Duration.ofSeconds(maxStored);
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public void updateMaxStored(final PointLocation location,
                                final Duration duration)
            throws InterruptedException {
        final String locationId = location.getIdentifier();
        final int durationSeconds = (int) duration.getSeconds();

        try {
            maxTimeStore.put(new LocationKey(locationId), durationSeconds);
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public Map<PointLocation, WalkingCosts> get(
            final PointLocation location, final Duration duration)
            throws InterruptedException {
        final String locationId = location.getIdentifier();
        final int durationSeconds = (int) duration.getSeconds();
        try {
            final NavigableSet<LocationTimeKey> values
                    = timeStore.getValuesBelow(LocationTimeKey.getMaxKey(
                            locationId, durationSeconds));
            final Map<PointLocation, WalkingCosts> cached = convertFromKeys(
                    values);
            return cached;
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public void putAll(final PointLocation location,
                       final Map<PointLocation, WalkingCosts> costs)
            throws InterruptedException {
        final String locationId = location.getIdentifier();
        timeStore.putAll(convertToKeys(locationId, costs));
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

}
