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
package com.publictransitanalytics.scoregenerator.datalayer.distanceestimates;

import com.bitvantage.bitvantagecaching.RangedStore;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Geodetic2DArc;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * Distance estimation directory that creates an exhaustive store under a
 * maximum distance.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class StoredDistanceEstimator implements DistanceEstimator {

    private final RangedStore<LocationDistanceKey, String> candidateDistancesStore;
    private final double maxDistanceMeters;

    public StoredDistanceEstimator(
            final Set<Sector> sectors, final Set<PointLocation> points,
            final double maxDistanceMeters,
            final RangedStore<LocationDistanceKey, String> candidateDistancesStore) {

        this.candidateDistancesStore = candidateDistancesStore;
        this.maxDistanceMeters = maxDistanceMeters;

        final ImmutableSet.Builder<VisitableLocation> builder
                = ImmutableSet.builder();
        builder.addAll(sectors);
        builder.addAll(points);
        final Set<VisitableLocation> terminals = builder.build();
        if (candidateDistancesStore.isEmpty()) {
            for (final PointLocation location : points) {
                for (final VisitableLocation secondLocation : terminals) {
                    final double distanceMeters = estimateDistanceMeters(
                            location.getCanonicalPoint(),
                            secondLocation.getCanonicalPoint());

                    if (distanceMeters <= maxDistanceMeters) {
                        final LocationDistanceKey key = LocationDistanceKey
                                .getWriteKey(location.getIdentifier(),
                                             distanceMeters);
                        candidateDistancesStore.put(key, secondLocation
                                                    .getIdentifier());
                    }
                }
            }
        }
    }

    @Override
    public Set<String> getReachableLocations(final String originLocationId,
                                             final double distanceMeters) {

        if (distanceMeters > maxDistanceMeters) {
            throw new ScoreGeneratorFatalException(String.format(
                    "Cannot get locations %f meters away; maximum distance is %f.",
                    distanceMeters, maxDistanceMeters));
        }

        final LocationDistanceKey key = LocationDistanceKey.getMaxKey(
                originLocationId, distanceMeters);

        final List<String> reachableLocations = candidateDistancesStore
                .getValuesBelow(key);

        return ImmutableSet.copyOf(reachableLocations);
    }

    private double estimateDistanceMeters(final Geodetic2DPoint a,
                                          final Geodetic2DPoint b) {
        final Geodetic2DArc arc = new Geodetic2DArc(b, a);
        return arc.getDistanceInMeters();
    }
}
