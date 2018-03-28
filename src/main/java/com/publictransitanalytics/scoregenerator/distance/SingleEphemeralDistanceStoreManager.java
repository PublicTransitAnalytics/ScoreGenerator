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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * @author Public Transit Analytics
 */
public class SingleEphemeralDistanceStoreManager
        implements DistanceStoreManager {

    private final NavigableMap<Duration, PointLocation> distancesFrom;
    private final Map<PointLocation, Duration> distancesTo;
    private final PointLocation singleLocation;
    private Duration maxStored;

    public SingleEphemeralDistanceStoreManager(final PointLocation location) {
        distancesFrom = Collections.synchronizedNavigableMap(new TreeMap<>());
        distancesTo = new ConcurrentHashMap<>();
        singleLocation = location;
        maxStored = Duration.ZERO;
    }

    @Override
    public Duration getMaxStored(final PointLocation location) throws
            InterruptedException {
        if (singleLocation.equals(location)) {
            return maxStored;
        } else {
            return distancesTo.get(location);
        }
    }

    @Override
    public void updateMaxStored(final PointLocation location,
                                final Duration duration)
            throws InterruptedException {
        if (singleLocation.equals(location)) {
            maxStored = duration;
        }
    }

    @Override
    public Map<PointLocation, WalkingCosts> get(final PointLocation location,
                                                final Duration duration)
            throws InterruptedException {

        if (singleLocation.equals(location)) {
            return distancesFrom.headMap(duration, true).entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getValue(),
                            entry -> new WalkingCosts(entry.getKey(), -1)));
        } else {
            return Collections.singletonMap(singleLocation, new WalkingCosts(
                                            distancesTo.get(location), -1));
        }
    }

    @Override
    public void putAll(final PointLocation location,
                       final Map<PointLocation, WalkingCosts> costs)
            throws InterruptedException {
        if (singleLocation.equals(location)) {
            distancesFrom.putAll(costs.entrySet().stream().collect(
                    Collectors.toMap(entry -> entry.getValue().getDuration(), 
                                     entry-> entry.getKey())));
        }
    }

}
