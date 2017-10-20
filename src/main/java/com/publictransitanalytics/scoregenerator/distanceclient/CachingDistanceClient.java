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

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.Store;
import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.distanceclient.types.DistanceCacheKey;
import com.publictransitanalytics.scoregenerator.distanceclient.types.WalkingDistanceMeasurement;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
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

    private final PointOrdererFactory ordererFactory;
    private final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache;
    private final DistanceClient client;

    @Override
    public Map<VisitableLocation, WalkingCosts>
            getDistances(final VisitableLocation point,
                         final Set<VisitableLocation> consideredPoints) throws
            DistanceClientException, InterruptedException {

        final ImmutableMap.Builder<VisitableLocation, WalkingCosts> resultBuilder
                = ImmutableMap.builder();
        final ImmutableSet.Builder<VisitableLocation> uncachedPointsBuilder
                = ImmutableSet.builder();

        for (VisitableLocation consideredPoint : consideredPoints) {
            final PointOrderer orderer = ordererFactory.getOrderer(
                    point, consideredPoint);
            final VisitableLocation origin = orderer.getOrigin();
            final VisitableLocation destination = orderer.getDestination();

            final DistanceCacheKey key = new DistanceCacheKey(
                    origin.getIdentifier(), destination.getIdentifier());
            try {
                final WalkingDistanceMeasurement measurement
                        = cache.get(key);

                if (measurement != null) {
                    final WalkingCosts costs = new WalkingCosts(
                            measurement.getDuration(),
                            measurement.getDistanceMeters());
                    resultBuilder.put(consideredPoint, costs);
                } else {
                    uncachedPointsBuilder.add(consideredPoint);
                }
            } catch (final BitvantageStoreException e) {
                throw new DistanceClientException(e);
            }
        }

        final Map<VisitableLocation, WalkingCosts> uncachedCosts
                = client.getDistances(point, uncachedPointsBuilder.build());
        resultBuilder.putAll(uncachedCosts);
        for (final Map.Entry<VisitableLocation, WalkingCosts> entry
                     : uncachedCosts.entrySet()) {
            final PointOrderer orderer = ordererFactory.getOrderer(
                    point, entry.getKey());

            final DistanceCacheKey cacheKey = new DistanceCacheKey(
                    orderer.getOrigin().getIdentifier(),
                    orderer.getDestination().getIdentifier());
            final WalkingCosts costs = entry.getValue();

            final WalkingDistanceMeasurement measurement
                    = new WalkingDistanceMeasurement(
                            costs.getDuration(), costs.getDistanceMeters());
            try {
                cache.put(cacheKey, measurement);
            } catch (final BitvantageStoreException e) {
                throw new DistanceClientException(e);
            }
        }

        return resultBuilder.build();
    }

    @Override
    public void close() {
        cache.close();
    }
}
