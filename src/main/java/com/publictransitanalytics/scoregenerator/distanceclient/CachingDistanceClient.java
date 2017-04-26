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

import com.bitvantage.bitvantagecaching.Cache;
import com.publictransitanalytics.scoregenerator.distanceclient.types.DistanceCacheKey;
import com.publictransitanalytics.scoregenerator.distanceclient.types.WalkingDistanceMeasurement;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * DistanceClient that uses a cache for fast lookups and defers to another
 * DistanceClient when the needed distance is unavailable.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class CachingDistanceClient implements DistanceClient {

    private final Cache<DistanceCacheKey, WalkingDistanceMeasurement> cache;
    private final DistanceClient client;

    @Override
    public Table<VisitableLocation, VisitableLocation, WalkingCosts>
            getDistances(final Set<VisitableLocation> origins,
                         final Set<VisitableLocation> destinations) throws
            DistanceClientException, InterruptedException {

        final ImmutableTable.Builder<VisitableLocation, VisitableLocation, WalkingCosts> resultBuilder
                = ImmutableTable.builder();
        final ImmutableSet.Builder<VisitableLocation> uncachedOriginsBuilder
                = ImmutableSet.builder();
        final ImmutableSet.Builder<VisitableLocation> uncachedDestinationsBuilder
                = ImmutableSet.builder();

        for (final VisitableLocation origin : origins) {
            for (VisitableLocation destination : destinations) {
                final DistanceCacheKey key = new DistanceCacheKey(
                        origin.getIdentifier(), destination.getIdentifier());
                final WalkingDistanceMeasurement measurement = cache.get(key);
                if (measurement != null) {
                    final WalkingCosts costs = new WalkingCosts(
                            measurement.getDuration(),
                            measurement.getDistanceMeters());
                    resultBuilder.put(origin, destination, costs);
                } else {
                    uncachedOriginsBuilder.add(origin);
                    uncachedDestinationsBuilder.add(destination);
                }
            }
        }

        final Table<VisitableLocation, VisitableLocation, WalkingCosts> uncachedCosts
                = client.getDistances(uncachedOriginsBuilder.build(),
                                      uncachedDestinationsBuilder.build());
        resultBuilder.putAll(uncachedCosts);
        for (final Table.Cell<VisitableLocation, VisitableLocation, WalkingCosts> cell
             : uncachedCosts.cellSet()) {

            final DistanceCacheKey cacheKey
                    = new DistanceCacheKey(cell.getRowKey().getIdentifier(),
                                           cell.getColumnKey().getIdentifier());
            final WalkingCosts costs = cell.getValue();

            final WalkingDistanceMeasurement measurement
                    = new WalkingDistanceMeasurement(
                            costs.getDuration(), costs.getDistanceMeters());

            cache.put(cacheKey, measurement);
        }

        return resultBuilder.build();
    }
}
