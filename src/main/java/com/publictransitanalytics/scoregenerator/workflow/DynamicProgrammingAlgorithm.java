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
package com.publictransitanalytics.scoregenerator.workflow;

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.visitors.FlatTransitRideVisitor;
import com.publictransitanalytics.scoregenerator.visitors.FlatWalkVisitor;
import com.publictransitanalytics.scoregenerator.visitors.ModeInfo;
import com.publictransitanalytics.scoregenerator.visitors.ReachabilityOutput;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import java.util.Collections;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;

/**
 * Dynamic programming algorithm for path finding.
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicProgrammingAlgorithm {

    public Map<VisitableLocation, DynamicProgrammingRecord> getMap(
            final LocalDateTime startTime,
            final LocalDateTime cutoffTime,
            final VisitableLocation startLocation,
            final TimeTracker timeTracker,
            final Duration duration, final int depth,
            final ReachabilityClient reachabilityClient,
            final RiderFactory riderFactory) throws
            InterruptedException {

        Map<VisitableLocation, DynamicProgrammingRecord> stateMap;
        Set<VisitableLocation> updateSet;

        final DynamicProgrammingRecord initialRecord
                = new DynamicProgrammingRecord(
                        startTime, ModeInfo.NONE, null);

        stateMap = new HashMap<>();
        stateMap.put(startLocation, initialRecord);
        updateSet = Collections.singleton(startLocation);

        for (int i = 0;; i++) {

            int roundUpdates = 0;
            final ImmutableSet.Builder<VisitableLocation> updateSetBuilder
                    = ImmutableSet.builder();
            for (final VisitableLocation priorLocation : updateSet) {

                final DynamicProgrammingRecord priorRecord
                        = stateMap.get(priorLocation);
                final LocalDateTime currentTime = priorRecord.getReachTime();

                final ImmutableSet.Builder<ReachabilityOutput> reachabilitiesBuilder
                        = ImmutableSet.builder();

                final FlatTransitRideVisitor transitRideVisitor
                        = new FlatTransitRideVisitor(cutoffTime, cutoffTime,
                                                     currentTime, riderFactory);
                priorLocation.accept(transitRideVisitor);
                final Set<ReachabilityOutput> transitRides
                        = transitRideVisitor.getOutput();
                reachabilitiesBuilder.addAll(transitRides);

                if (!priorRecord.getMode().getType().equals(ModeType.WALKING)) {
                    final FlatWalkVisitor walkVisitor = new FlatWalkVisitor(
                            cutoffTime, currentTime, reachabilityClient,
                            timeTracker);
                    priorLocation.accept(walkVisitor);
                    final Set<ReachabilityOutput> walks
                            = walkVisitor.getOutput();
                    reachabilitiesBuilder.addAll(walks);
                }

                updateSetBuilder.addAll(updateRow(
                        reachabilitiesBuilder.build(), stateMap, priorLocation,
                        timeTracker));
                roundUpdates += updateSet.size();
            }

            if (roundUpdates == 0) {
                log.debug(
                        "Stopped processing at round {} because no updates.",
                        i);
                break;
            }
            updateSet = updateSetBuilder.build();
        }
        return stateMap;
    }

    private Set<VisitableLocation> updateRow(
            final Set<ReachabilityOutput> reachabilities,
            final Map<VisitableLocation, DynamicProgrammingRecord> stateMap,
            final VisitableLocation priorLocation,
            final TimeTracker timeTracker) {
        final ImmutableSet.Builder<VisitableLocation> builder
                = ImmutableSet.builder();
        for (final ReachabilityOutput reachability : reachabilities) {
            final VisitableLocation newLocation = reachability.getLocation();
            final LocalDateTime newTime = reachability.getReachTime();

            final ModeInfo mode = reachability.getModeInfo();

            final DynamicProgrammingRecord record
                    = new DynamicProgrammingRecord(
                            newTime, mode, priorLocation);

            if (stateMap.containsKey(newLocation)) {
                final DynamicProgrammingRecord earlierThisRoundReach
                        = stateMap.get(newLocation);
                final LocalDateTime earlierThisRoundTime
                        = earlierThisRoundReach.getReachTime();

                if (timeTracker.shouldReplace(earlierThisRoundTime, newTime)) {
                    stateMap.put(newLocation, record);
                    builder.add(newLocation);
                }
            } else {
                stateMap.put(newLocation, record);
                builder.add(newLocation);
            }
        }
        return builder.build();
    }
}
