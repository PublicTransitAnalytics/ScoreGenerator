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
package com.publictransitanalytics.scoregenerator.schedule.patching;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.distance.CompositeDistanceStoreManager;
import com.publictransitanalytics.scoregenerator.distance.DistanceClient;
import com.publictransitanalytics.scoregenerator.distance.DistanceStoreManager;
import com.publictransitanalytics.scoregenerator.distance.RangedCachingReachabilityClient;
import com.publictransitanalytics.scoregenerator.distance.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.distance.SingleEphemeralDistanceStoreManager;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripCreatingTransitNetwork;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class Transformer {

    private final List<Patch> tripPatches;
    private final Set<TransitStop> addedStops;
    private final TimeTracker timeTracker;
    private final TransitNetwork originalTransitNetwork;
    private final boolean backward;
    private final Duration longestDuration;
    private final double walkingMetersPerSecond;
    private final DistanceClient distanceClient;
    private final DistanceClient estimationClient;
    private final ReachabilityClient originalReachabilityClient;
    private final RiderFactory originalRiderFactory;
    private final DistanceStoreManager storeManager;
    private final BiMap<String, PointLocation> pointIdMap;

    public Transformer(final TimeTracker timeTracker,
                       final TransitNetwork transitNetwork,
                       final boolean backward, final Duration longestDuration,
                       final double walkingMetersPerSecond,
                       final DistanceClient distanceClient,
                       final DistanceClient estimationClient,
                       final BiMap<String, PointLocation> pointIdMap,
                       final ReachabilityClient reachabilityClient,
                       final DistanceStoreManager storeManager,
                       final RiderFactory riderFactory
    ) {

        this.timeTracker = timeTracker;
        originalTransitNetwork = transitNetwork;
        this.backward = backward;
        this.longestDuration = longestDuration;
        this.walkingMetersPerSecond = walkingMetersPerSecond;
        this.distanceClient = distanceClient;
        this.estimationClient = estimationClient;
        this.storeManager = storeManager;
        this.pointIdMap = pointIdMap;
        originalReachabilityClient = reachabilityClient;
        originalRiderFactory = riderFactory;

        tripPatches = new ArrayList<>();
        addedStops = new HashSet<>();
    }

    public void addTripPatch(final Patch patch) {
        tripPatches.add(patch);
    }

    public void addStop(final TransitStop stop) {
        addedStops.add(stop);
    }

    public TransitNetwork getTransitNetwork() throws InterruptedException {

        return new TripCreatingTransitNetwork(new PatchingTripCreator(
                tripPatches, originalTransitNetwork));
    }

    public RiderFactory getRiderFactory() throws InterruptedException {
        // TODO: Account for direction changes
        final TransitNetwork transitNetwork = getTransitNetwork();
        if (transitNetwork != originalTransitNetwork) {
            return backward ? new RetrospectiveRiderFactory(transitNetwork)
                    : new ForwardRiderFactory(transitNetwork);
        } else {
            return originalRiderFactory;
        }

    }

    public ReachabilityClient getReachabilityClient()
            throws InterruptedException {
        final ReachabilityClient client;
        if (addedStops.isEmpty()) {
            client = originalReachabilityClient;
        } else {
            final Map<PointLocation, DistanceStoreManager> supplementalStores
                    = addedStops.stream().collect(Collectors.toMap(
                            Function.identity(), location
                            -> new SingleEphemeralDistanceStoreManager(
                                    location)));

            final DistanceStoreManager store
                    = new CompositeDistanceStoreManager(supplementalStores,
                                                        storeManager);

            final ImmutableSet<PointLocation> points
                    = ImmutableSet.<PointLocation>builder()
                            .addAll(pointIdMap.values())
                            .addAll(addedStops).build();

            client = new RangedCachingReachabilityClient(
                    store, points, timeTracker, distanceClient,
                    estimationClient);
        }

        return client;
    }

}
