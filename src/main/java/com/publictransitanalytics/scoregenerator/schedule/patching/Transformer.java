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

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.distanceclient.CompositeDistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateRefiningReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.SingletonEphemeralEstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.PairGenerator;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.StoredDistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.SupplementalPairGenerator;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripProcessingTransitNetwork;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final Set<GridPoint> gridPoints;
    private final Set<TransitStop> stops;
    private final Set<PointLocation> centers;
    private final DistanceEstimator originalDistanceEstimator;
    private final ReachabilityClient originalReachabilityClient;
    private final RiderFactory originalRiderFactory;

    public Transformer(final TimeTracker timeTracker,
                       final TransitNetwork transitNetwork,
                       final boolean backward, final Duration longestDuration,
                       final double walkingMetersPerSecond,
                       final DistanceClient distanceClient,
                       final Set<GridPoint> gridPoints,
                       final Set<TransitStop> stops,
                       final Set<PointLocation> centers,
                       final DistanceEstimator distanceEstimator,
                       final ReachabilityClient reachabilityClient,
                       final RiderFactory riderFactory) {

        this.timeTracker = timeTracker;
        originalTransitNetwork = transitNetwork;
        this.backward = backward;
        this.longestDuration = longestDuration;
        this.walkingMetersPerSecond = walkingMetersPerSecond;
        this.distanceClient = distanceClient;
        this.stops = stops;
        this.centers = centers;
        this.gridPoints = gridPoints;
        originalDistanceEstimator = distanceEstimator;
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

        return new TripProcessingTransitNetwork(new PatchingTripCreator(
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

    public DistanceEstimator getDistanceEstimator()
            throws InterruptedException {
        if (addedStops.isEmpty()) {
            return originalDistanceEstimator;
        } else {
            final ImmutableSet.Builder<DistanceEstimator> builder
                    = ImmutableSet.builder();
            final double maxDistance
                    = longestDuration.getSeconds() * walkingMetersPerSecond;
            builder.add(originalDistanceEstimator);
            for (final TransitStop stop : addedStops) {
                final EstimateStorage storage
                        = new SingletonEphemeralEstimateStorage(stop);
                final Set<TransitStop> allStops
                        = ImmutableSet.<TransitStop>builder()
                                .addAll(stops).addAll(addedStops).build();

                final PairGenerator pairGenerator
                        = new SupplementalPairGenerator(
                                gridPoints, allStops, centers,
                                Collections.singleton(stop));

                final StoredDistanceEstimator addedLocationEstimator
                        = new StoredDistanceEstimator(
                                pairGenerator, maxDistance, storage);
                builder.add(addedLocationEstimator);
            }
            return new CompositeDistanceEstimator(builder.build());
        }
    }

    public ReachabilityClient getReachabilityClient()
            throws InterruptedException {
        if (addedStops.isEmpty()) {
            return originalReachabilityClient;
        } else {
            return new EstimateRefiningReachabilityClient(
                    distanceClient, getDistanceEstimator(), timeTracker,
                    walkingMetersPerSecond);
        }
    }

}
