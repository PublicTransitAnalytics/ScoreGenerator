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

import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Public Transit Analytics
 */
public class SingletonEphemeralEstimateStorage implements EstimateStorage {

    private final PointLocation singleOrigin;
    private final TreeMultimap<Double, PointLocation> distances;
    private final Map<PointLocation, Double> reverseMap;

    public SingletonEphemeralEstimateStorage(final PointLocation origin) {
        singleOrigin = origin;
        distances = TreeMultimap.create(
                Comparator.naturalOrder(),
                (l1, l2) -> l1.getIdentifier().compareTo(l2.getIdentifier()));
        reverseMap = new HashMap<>();
    }

    @Override
    public void put(final PointLocation origin,
                    final PointLocation destination,
                    final double distanceMeters) throws InterruptedException {
        if (origin.equals(singleOrigin)) {
            distances.put(distanceMeters, destination);
        } else if (destination.equals(singleOrigin)) {
            reverseMap.put(origin, distanceMeters);
        }
    }

    @Override
    public Set<PointLocation> getReachable(final PointLocation origin,
                                               final double distanceMeters)
            throws InterruptedException {
        if (origin.equals(singleOrigin)) {
            return distances.asMap().headMap(distanceMeters, true).values()
                    .stream().flatMap(values -> values.stream())
                    .collect(Collectors.toSet());
        } else if (reverseMap.containsKey(origin)) {
            final double distance = reverseMap.get(origin);
            if (distance <= distanceMeters) {
                return Collections.singleton(singleOrigin);
            } else {
                return Collections.emptySet();
            }
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean isInitialized() throws InterruptedException {
        return false;
    }

    @Override
    public double getMaxStored(PointLocation origin) throws InterruptedException {
        return 0;
    }

    @Override
    public void updateMaxStored(PointLocation orgin, double max) throws
            InterruptedException {
    }

    @Override
    public void close() {
    }

}
