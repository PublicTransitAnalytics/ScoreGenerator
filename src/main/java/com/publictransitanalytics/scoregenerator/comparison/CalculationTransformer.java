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

import com.google.common.collect.ImmutableSet;
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
import com.publictransitanalytics.scoregenerator.schedule.patching.Patch;
import com.publictransitanalytics.scoregenerator.schedule.patching.PatchingTripCreator;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripProcessingTransitNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class CalculationTransformer {

    private final Calculation original;
    private final ScoreCardFactory scoreCardFactory;
    private final List<Patch> tripPatches;
    private final Set<TransitStop> addedStops;

    public CalculationTransformer(final Calculation original,
                                  final ScoreCardFactory scoreCardFactory) {
        this.original = original;
        this.scoreCardFactory = scoreCardFactory;
        tripPatches = new ArrayList<>();
        addedStops = new HashSet<>();
    }

    public void addTripPatch(final Patch patch) {
        tripPatches.add(patch);
    }

    public void addStop(final TransitStop stop) {
        addedStops.add(stop);
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
                original.getEndpointDeterminer(), original.getDistanceClient(),
                original.getSectors(), original.getStops(),
                original.getCenters(), newDistanceEstimator,
                newReachabilityClient, newRiderFactory);
    }

    private TransitNetwork getPatchedNetwork() throws InterruptedException {

        return new TripProcessingTransitNetwork(new PatchingTripCreator(
                tripPatches, original.getTransitNetwork()));
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
            final double maxDistance
                    = original.getLongestDuration().getSeconds() * 
                      original.getWalkingDistanceMetersPerSecond();
            builder.add(original.getDistanceEstimator());
            for (final TransitStop stop : addedStops) {
                final EstimateStorage storage
                        = new SingletonEphemeralEstimateStorage(stop);
                final Set<TransitStop> stops = ImmutableSet.builder().addAll(
                        original.getStops()).addAll(addedStops).build();

                final PairGenerator pairGenerator
                        = new SupplementalPairGenerator(
                                original.getSectors(), stops,
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
                    original.getDistanceClient(), newEstimator,
                    original.getTimeTracker(),
                    original.getWalkingDistanceMetersPerSecond());
        }
    }

}
