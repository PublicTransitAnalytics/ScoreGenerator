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
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.visitors.FlatTransitRideVisitor;
import com.publictransitanalytics.scoregenerator.visitors.FlatWalkVisitor;
import com.publictransitanalytics.scoregenerator.visitors.ModeInfo;
import com.publictransitanalytics.scoregenerator.visitors.ReachabilityOutput;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.util.HashMap;

/**
 * Task used by the dynamic programming workflow.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
@Slf4j
public class ProgressiveRangeExecutor implements RangeExecutor {

    private final DynamicProgrammingAlgorithm algorithm;
    private final Environment environment;

    @Override
    public void executeRange(final Calculation calculation,
                             final TaskGroupIdentifier taskGroup)
            throws InterruptedException {
        final Duration duration = environment.getLongestDuration();

        final ScoreCard scoreCard = calculation.getScoreCard();
        final TimeTracker timeTracker = calculation.getTimeTracker();
        final RiderFactory riderFactory = calculation.getRiderFactory();
        final ReachabilityClient reachabilityClient
                = calculation.getReachabilityClient();

        final Instant profileStartTime = Instant.now();
        final PointLocation startLocation = taskGroup.getCenter();

        final Iterator<LocalDateTime> timeIterator
                = timeTracker.getTimeIterator(calculation.getTimes());

        final LocalDateTime latestStartTime = timeIterator.next();
        final LocalDateTime latestCutoffTime = timeTracker.adjust(
                latestStartTime, duration);

        final AlgorithmOutput output = algorithm.getOutput(
                latestStartTime, latestCutoffTime, startLocation, timeTracker,
                duration, reachabilityClient, riderFactory);
        Map<VisitableLocation, DynamicProgrammingRecord> map
                = output.getMap();

        final TaskIdentifier latestFullTask = new TaskIdentifier(
                latestStartTime, startLocation);

        final MovementAssembler assembler = calculation.getMovementAssembler();

        updateScoreCard(map, latestFullTask, scoreCard, assembler);

        final Map<VisitableLocation, WalkingCosts> initialWalks
                = output.getInitialWalks();
        final Set<VisitableLocation> implicitLocations
                = output.getImplicitLocations();

        while (timeIterator.hasNext()) {

            final LocalDateTime nextStartTime = timeIterator.next();
            final LocalDateTime nextCutoffTime = timeTracker.adjust(
                    nextStartTime, duration);

            final Map<VisitableLocation, DynamicProgrammingRecord> nextMap
                    = createNextMap(map, nextStartTime, nextCutoffTime,
                                    startLocation, riderFactory,
                                    reachabilityClient, timeTracker,
                                    initialWalks, implicitLocations);

            final TaskIdentifier nextTask = new TaskIdentifier(
                    nextStartTime, startLocation);

            updateScoreCard(nextMap, nextTask, scoreCard, assembler);
            map = nextMap;
        }
        final Instant profileEndTime = Instant.now();

        log.info("Finished {} at {} (wallclock {}).", taskGroup,
                 profileEndTime.toString(),
                 Duration.between(profileStartTime, profileEndTime));

    }

    private Map<VisitableLocation, DynamicProgrammingRecord> createNextMap(
            final Map<VisitableLocation, DynamicProgrammingRecord> previousMap,
            final LocalDateTime startTime, final LocalDateTime cutoffTime,
            final VisitableLocation startLocation,
            final RiderFactory riderFactory,
            final ReachabilityClient reachabilityClient,
            final TimeTracker timeTracker,
            final Map<VisitableLocation, WalkingCosts> initialWalks,
            final Set<VisitableLocation> implicitLocations)
            throws InterruptedException {

        final Map<VisitableLocation, DynamicProgrammingRecord> stateMap
                = new HashMap<>(previousMap.size());
        for (final Map.Entry<VisitableLocation, DynamicProgrammingRecord> entry
                     : previousMap.entrySet()) {

            final DynamicProgrammingRecord record = entry.getValue();

            if (timeTracker.meetsCutoff(record.getReachTime(), cutoffTime)) {
                stateMap.put(entry.getKey(), record);
            }
        }

        final DynamicProgrammingRecord newStartRecord
                = new DynamicProgrammingRecord(startTime, ModeInfo.NONE, null);
        stateMap.put(startLocation, newStartRecord);

        final ImmutableSet.Builder<VisitableLocation> initialUpdateSetBuilder
                = ImmutableSet.builder();
        for (final Map.Entry<VisitableLocation, WalkingCosts> entry
                     : initialWalks.entrySet()) {
            final WalkingCosts walk = entry.getValue();
            final LocalDateTime newReachTime = timeTracker.adjust(
                    startTime, walk.getDuration());

            final VisitableLocation location = entry.getKey();
            final ModeInfo newModeInfo = new ModeInfo(ModeType.WALKING, null,
                                                      walk);
            final DynamicProgrammingRecord newWalkRecord
                    = new DynamicProgrammingRecord(
                            newReachTime, newModeInfo, startLocation);

            if (!stateMap.containsKey(location)) {
                stateMap.put(location, newWalkRecord);
                initialUpdateSetBuilder.add(location);
            } else {
                final LocalDateTime reachTime
                        = stateMap.get(location).getReachTime();
                
                if (timeTracker.shouldReplace(reachTime, newReachTime)) {
                    stateMap.put(location, newWalkRecord);
                    initialUpdateSetBuilder.add(location);
                }
            }
        }
        for (final VisitableLocation location : implicitLocations) {
            final ModeInfo newModeInfo = new ModeInfo(ModeType.NONE, null,
                                                      null);
            final DynamicProgrammingRecord newRecord
                    = new DynamicProgrammingRecord(startTime, newModeInfo,
                                                   startLocation);
            initialUpdateSetBuilder.add(location);
            stateMap.put(location, newRecord);
        }

        Set<VisitableLocation> updateSet = initialUpdateSetBuilder.build();

        for (int i = 1;; i++) {

            int roundUpdates = 0;
            final ImmutableSet.Builder<VisitableLocation> updateSetBuilder
                    = ImmutableSet.builder();
            for (final VisitableLocation priorLocation : updateSet) {
                final DynamicProgrammingRecord priorRecord
                        = stateMap.get(priorLocation);
                final LocalDateTime newReachTime = priorRecord.getReachTime();

                final DynamicProgrammingRecord previousPriorRecord
                        = previousMap.get(priorLocation);
                final LocalDateTime waitCutoffTime;
                if (previousPriorRecord == null) {
                    waitCutoffTime = cutoffTime;
                } else {
                    waitCutoffTime = previousPriorRecord.getReachTime();
                }

                final ImmutableSet.Builder<ReachabilityOutput> reachabilitiesBuilder
                        = ImmutableSet.builder();
                final FlatTransitRideVisitor transitRideVisitor
                        = new FlatTransitRideVisitor(cutoffTime, waitCutoffTime,
                                                     newReachTime,
                                                     riderFactory);
                priorLocation.accept(transitRideVisitor);
                final Set<ReachabilityOutput> transitRides = transitRideVisitor
                        .getOutput();
                reachabilitiesBuilder.addAll(transitRides);

                if (!priorRecord.getMode().getType().equals(ModeType.WALKING)) {
                    final FlatWalkVisitor walkVisitor = new FlatWalkVisitor(
                            cutoffTime, newReachTime,
                            reachabilityClient, timeTracker);
                    priorLocation.accept(walkVisitor);
                    final Set<ReachabilityOutput> walks
                            = walkVisitor.getOutput();
                    reachabilitiesBuilder.addAll(walks);
                }

                updateSetBuilder.addAll(updateRow(reachabilitiesBuilder.build(),
                                                  stateMap, priorLocation,
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

    private static Set<VisitableLocation> updateRow(
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

            final DynamicProgrammingRecord newRecord
                    = new DynamicProgrammingRecord(
                            newTime, mode, priorLocation);

            if (stateMap.containsKey(newLocation)) {
                final DynamicProgrammingRecord currentRecord
                        = stateMap.get(newLocation);
                if (timeTracker.shouldReplace(currentRecord.getReachTime(),
                                              newTime)) {
                    stateMap.put(newLocation, newRecord);
                    builder.add(newLocation);
                }
            } else {
                stateMap.put(newLocation, newRecord);
                builder.add(newLocation);
            }

        }
        return builder.build();
    }

    private void updateScoreCard(
            final Map<VisitableLocation, DynamicProgrammingRecord> map,
            final TaskIdentifier task, final ScoreCard scoreCard,
            final MovementAssembler assembler)
            throws InterruptedException {

        final Set<VisitableLocation> reachedLocations = map.keySet();

        for (final VisitableLocation reachedLocation : reachedLocations) {
            final MovementPath path = assembler.assemble(reachedLocation, map);
            scoreCard.putPath(reachedLocation, task, path);
        }
    }

}
