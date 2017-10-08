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
package com.publictransitanalytics.scoregenerator.comparison;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.publictransitanalytics.scoregenerator.distanceclient.CompositeDistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateRefiningReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.SingletonEphemeralEstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.PairGenerator;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.StoredDistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.SupplementalPairGenerator;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.workflow.Calculation;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.schedule.PatchingTripCreator;
import com.publictransitanalytics.scoregenerator.schedule.RouteExtension;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripProcessingTransitNetwork;
import java.util.Collections;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class CalculationTransformer {

    private final Calculation original;
    private final ScoreCardFactory scoreCardFactory;
    private final Set<String> removedRoutes;
    private final ListMultimap<String, RouteExtension> extendedRoutes;
    private final Set<TransitStop> addedStops;

    public CalculationTransformer(final Calculation original,
                                  final ScoreCardFactory scoreCardFactory) {
        this.original = original;
        this.scoreCardFactory = scoreCardFactory;
        removedRoutes = new HashSet<>();
        extendedRoutes = ArrayListMultimap.create();
        addedStops = new HashSet<>();
    }

    public void deleteRoute(final String route) {
        removedRoutes.add(route);
    }

    public void addStop(final TransitStop stop) {
        addedStops.add(stop);
    }

    public void extend(final String route, final RouteExtension extension) {
        extendedRoutes.put(route, extension);
    }

    public Calculation transform() throws InterruptedException {
        final TransitNetwork patchedNetwork = getPatchedNetwork();
        final RiderFactory newRiderFactory = getRiderFactory(patchedNetwork);
        final int newTaskCount = original.getTaskCount();
        final DistanceEstimator newDistanceEstimator = getDistanceEstimator();
        final ReachabilityClient newReachabilityClient
                = getReachabilityClient(newDistanceEstimator);

        return new Calculation(
                original.getTaskGroups(), original.getTimes(),
                scoreCardFactory.makeScoreCard(newTaskCount),
                original.getTimeTracker(), original.getMovementAssembler(),
                original.getAllowedModes(), patchedNetwork,
                original.isBackward(), original.getLongestDuration(),
                original.getWalkingDistanceMetersPerSecond(),
                original.getEndpointDeterminer(), original.getDistanceFilter(),
                original.getSectors(), original.getStops(), 
                original .getCenters(), newDistanceEstimator, 
                newReachabilityClient, newRiderFactory);
    }

    private TransitNetwork getPatchedNetwork() throws InterruptedException {

        return new TripProcessingTransitNetwork(new PatchingTripCreator(
                removedRoutes, extendedRoutes, original.getTransitNetwork()));
    }

    private RiderFactory getRiderFactory(final TransitNetwork newNetwork) {
        // TODO: Account for direction changes
        if (newNetwork == original.getTransitNetwork()) {
            return original.getRiderFactory();
        } else {
            return original.isBackward() ? new RetrospectiveRiderFactory(
                    newNetwork) : new ForwardRiderFactory(newNetwork);
        }

    }

    private DistanceEstimator getDistanceEstimator()
            throws InterruptedException {
        if (addedStops.isEmpty()) {
            return original.getDistanceEstimator();
        } else {
            final ImmutableSet.Builder<DistanceEstimator> builder
                    = ImmutableSet.builder();
            builder.add(original.getDistanceEstimator());
            for (final TransitStop stop : addedStops) {
                final double maxDistance
                        = original.getLongestDuration().getSeconds() * original
                        .getWalkingDistanceMetersPerSecond();
                final EstimateStorage storage
                        = new SingletonEphemeralEstimateStorage(stop);
                final PairGenerator pairGenerator
                        = new SupplementalPairGenerator(
                                original.getSectors(), original.getStops(),
                                original.getCenters(),
                                Collections.singleton(stop));

                final StoredDistanceEstimator addedLocationEstimator
                        = new StoredDistanceEstimator(
                                pairGenerator, maxDistance,
                                original.getEndpointDeterminer(), storage);
                builder.add(addedLocationEstimator);
            }
            return new CompositeDistanceEstimator(builder.build());
        }
    }

    private ReachabilityClient getReachabilityClient(
            final DistanceEstimator newEstimator) {
        if (addedStops.isEmpty()) {
            return original.getReachabilityClient();
        } else {
            return new EstimateRefiningReachabilityClient(
                    original.getDistanceFilter(), newEstimator,
                    original.getTimeTracker(),
                    original.getWalkingDistanceMetersPerSecond());
        }
    }

}
