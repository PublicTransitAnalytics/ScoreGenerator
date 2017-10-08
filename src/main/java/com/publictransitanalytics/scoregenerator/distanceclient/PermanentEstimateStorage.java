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
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationDistanceKey;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationKey;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import java.util.Set;
import java.util.SortedMap;

/**
 *
 * @author Public Transit Analytics
 */
public class PermanentEstimateStorage implements EstimateStorage {

    private final RangedStore<LocationDistanceKey, String> candidateDistancesStore;
    private final Store<LocationKey, Double> maxCandidateDistanceStore;
    private final BiMap<String, VisitableLocation> locationMap;

    public PermanentEstimateStorage(
            final Store<LocationKey, Double> maxCandidateDistanceStore,
            final RangedStore<LocationDistanceKey, String> candidateDistancesStore,
            final BiMap<String, VisitableLocation> locationMap) {

        this.candidateDistancesStore = candidateDistancesStore;
        this.maxCandidateDistanceStore = maxCandidateDistanceStore;
        this.locationMap = locationMap;
    }

    @Override
    public void put(final PointLocation origin, 
                    final VisitableLocation destination,
                    final double distanceMeters) throws InterruptedException {
        final LocationDistanceKey key = LocationDistanceKey
                .getWriteKey(origin.getIdentifier(),
                             distanceMeters);
        try {
            candidateDistancesStore.put(key, destination.getIdentifier());
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public Set<VisitableLocation> getReachable(final PointLocation origin,
                                               final double distanceMeters)
            throws InterruptedException {
        final LocationDistanceKey key = LocationDistanceKey.getMaxKey(
                origin.getIdentifier(), distanceMeters);
        try {
            final SortedMap<LocationDistanceKey, String> reachableLocations
                    = candidateDistancesStore.getValuesBelow(key);
            final ImmutableSet.Builder<VisitableLocation> builder
                    = ImmutableSet.builder();
            for (final String locationIdentifier
                         : reachableLocations.values()) {
                final VisitableLocation location
                        = locationMap.get(locationIdentifier);
                if (location != null) {
                    builder.add(location);
                }
            }
            return builder.build();
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public boolean isInitialized() throws InterruptedException {
        try {
            return !candidateDistancesStore.isEmpty();
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public double getMaxStored(final PointLocation origin)
            throws InterruptedException {
        try {
            final Double max = maxCandidateDistanceStore.get(
                    new LocationKey(origin.getIdentifier()));
            return max == null ? 0 : max;
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public void updateMaxStored(final PointLocation origin, final double max)
            throws InterruptedException {
        try {
            maxCandidateDistanceStore.put(
                    new LocationKey(origin.getIdentifier()), max);
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public void close() {
        maxCandidateDistanceStore.close();
        candidateDistancesStore.close();        
    }

}
