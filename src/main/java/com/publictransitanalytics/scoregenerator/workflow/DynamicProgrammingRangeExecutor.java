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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
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

/**
 * Task used by the dynamic programming workflow.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
@Slf4j
public class DynamicProgrammingRangeExecutor {

    private final DynamicProgrammingAlgorithm algorithm;
    private final Environment environment;

    public void executeRange(final Calculation calculation,
                             final TaskGroupIdentifier taskGroup)
            throws InterruptedException {
        final int maxDepth = environment.getMaxDepth();
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

        Table<Integer, VisitableLocation, DynamicProgrammingRecord> priorTable
                = algorithm.createTable(latestStartTime, latestCutoffTime,
                                        startLocation, timeTracker, duration,
                                        maxDepth, reachabilityClient,
                                        riderFactory);
        LocalDateTime priorCutoffTime = latestCutoffTime;

        final TaskIdentifier latestFullTask = new TaskIdentifier(
                latestStartTime, startLocation);

        final MovementAssembler assembler = calculation.getMovementAssembler();

        updateScoreCard(priorTable, latestFullTask, scoreCard, assembler);

        while (timeIterator.hasNext()) {
            final Table<Integer, VisitableLocation, DynamicProgrammingRecord> nextTable
                    = HashBasedTable.create();
            final LocalDateTime nextStartTime = timeIterator.next();
            final LocalDateTime nextCutoffTime = timeTracker.adjust(
                    nextStartTime, duration);

            createNextTable(priorTable, nextTable, nextStartTime,
                            nextCutoffTime, priorCutoffTime, riderFactory,
                            reachabilityClient, timeTracker);

            final TaskIdentifier nextTask = new TaskIdentifier(
                    nextStartTime, startLocation);

            updateScoreCard(nextTable, nextTask, scoreCard, assembler);
            priorTable = nextTable;
            priorCutoffTime = nextCutoffTime;
        }
        final Instant profileEndTime = Instant.now();

        log.info("Finished {} at {} (wallclock {}).", taskGroup,
                 profileEndTime.toString(),
                 Duration.between(profileStartTime, profileEndTime));

    }

    private void createNextTable(
            final Table<Integer, VisitableLocation, DynamicProgrammingRecord> priorTable,
            final Table<Integer, VisitableLocation, DynamicProgrammingRecord> newTable,
            final LocalDateTime startTime, final LocalDateTime cutoffTime,
            final LocalDateTime previousCutoffTime,
            final RiderFactory riderFactory,
            final ReachabilityClient reachabilityClient,
            final TimeTracker timeTracker) throws InterruptedException {

        final Map<VisitableLocation, DynamicProgrammingRecord> firstPriorRow
                = priorTable.row(0);
        for (final VisitableLocation priorTableLocation
                     : firstPriorRow.keySet()) {
            final DynamicProgrammingRecord newFirstRecord
                    = new DynamicProgrammingRecord(
                            startTime, ModeInfo.NONE, null);
            newTable.put(0, priorTableLocation, newFirstRecord);
        }

        for (int i = 1;; i++) {
            final Map<VisitableLocation, DynamicProgrammingRecord> newTablePreviousRow
                    = newTable.row(i - 1);
            final Map<VisitableLocation, DynamicProgrammingRecord> newTableCurrentRow
                    = newTable.row(i);
            final Map<VisitableLocation, DynamicProgrammingRecord> priorTablePreviousRow
                    = priorTable.row(i - 1);
            final Map<VisitableLocation, DynamicProgrammingRecord> priorTableCurrentRow
                    = priorTable.row(i);

            newTableCurrentRow.putAll(newTablePreviousRow);
            final int merges = mergeRowFromPriorTableRow(
                    priorTableCurrentRow, priorTablePreviousRow,
                    newTableCurrentRow, newTablePreviousRow, cutoffTime,
                    timeTracker);
            final int updates = updateFromPreviousRow(
                    newTableCurrentRow, newTablePreviousRow,
                    priorTablePreviousRow, cutoffTime, previousCutoffTime,
                    riderFactory, reachabilityClient, timeTracker);
            if (merges == 0 && updates == 0) {
                log.debug("Stopped processing at row {} because no updates.",
                          i);
                break;
            }
        }
    }

    private int updateFromPreviousRow(
            final Map<VisitableLocation, DynamicProgrammingRecord> newTableCurrentRow,
            final Map<VisitableLocation, DynamicProgrammingRecord> newTablePreviousRow,
            final Map<VisitableLocation, DynamicProgrammingRecord> priorTablePreviousRow,
            final LocalDateTime cutoffTime, final LocalDateTime priorCutoffTime,
            final RiderFactory riderFactory,
            final ReachabilityClient reachabilityClient,
            final TimeTracker timeTracker) throws InterruptedException {
        int replacements = 0;

        for (final VisitableLocation newTablePreviousRowLocation
                     : newTablePreviousRow.keySet()) {
            final DynamicProgrammingRecord newTablePreviousRowRecord
                    = newTablePreviousRow.get(newTablePreviousRowLocation);
            final LocalDateTime newTableReachTime
                    = newTablePreviousRowRecord.getReachTime();

            final Set<ReachabilityOutput> reachabilities;
            if (priorTablePreviousRow.containsKey(
                    newTablePreviousRowLocation)) {
                final DynamicProgrammingRecord priorTablePreviousRowRecord
                        = priorTablePreviousRow.get(newTablePreviousRowLocation);

                final LocalDateTime priorTableReachTime
                        = priorTablePreviousRowRecord.getReachTime();
                log.debug(
                        "{} was reached in previous table at {} and reached at {} in new table.",
                        newTablePreviousRowLocation, newTableReachTime,
                        priorTableReachTime);
                /* If we arrived at the prior stop earlier, check for bus trips
                   in that window. */
                final ImmutableSet.Builder<ReachabilityOutput> reachabilitiesBuilder
                        = ImmutableSet.builder();
                if (timeTracker.shouldReplace(priorTableReachTime,
                                              newTableReachTime)) {
                    log.debug(
                            "Generating transit trips at {} starting from {} to {} with cutoff at {}",
                            newTablePreviousRowLocation, newTableReachTime,
                            priorTableReachTime, cutoffTime);

                    final FlatTransitRideVisitor transitRideVisitor
                            = new FlatTransitRideVisitor(
                                    cutoffTime, priorTableReachTime,
                                    newTableReachTime, riderFactory);
                    newTablePreviousRowLocation.accept(transitRideVisitor);
                    final Set<ReachabilityOutput> transitRides
                            = transitRideVisitor.getOutput();
                    reachabilitiesBuilder.addAll(transitRides);
                }

                if (!newTablePreviousRowRecord.getMode().getType()
                        .equals(ModeType.WALKING)) {
                    /* If we have more walking time than previously, look for 
                       new  walks. */
                    final Duration priorWalkDuration = timeTracker.getDuration(
                            priorTableReachTime, priorCutoffTime);
                    final Duration walkDuration = timeTracker.getDuration(
                            newTableReachTime, cutoffTime);

                    if (walkDuration.compareTo(priorWalkDuration) > 0) {
                        log.debug(
                                "Generating walk from {} because there was {} duration rather than {}",
                                newTablePreviousRowLocation, priorWalkDuration,
                                walkDuration);
                        final FlatWalkVisitor walkVisitor = new FlatWalkVisitor(
                                cutoffTime, newTableReachTime,
                                reachabilityClient, timeTracker);
                        newTablePreviousRowLocation.accept(walkVisitor);
                        final Set<ReachabilityOutput> walks
                                = walkVisitor.getOutput();
                        reachabilitiesBuilder.addAll(walks);
                    }
                }
                reachabilities = reachabilitiesBuilder.build();
            } else {
                log.debug("Reached {} in new table and not in prior.",
                          newTablePreviousRowLocation);
                final ImmutableSet.Builder<ReachabilityOutput> reachabilitiesBuilder
                        = ImmutableSet.builder();
                final FlatTransitRideVisitor transitRideVisitor
                        = new FlatTransitRideVisitor(
                                cutoffTime, cutoffTime, newTableReachTime,
                                riderFactory);
                newTablePreviousRowLocation.accept(transitRideVisitor);
                final Set<ReachabilityOutput> transitRides
                        = transitRideVisitor.getOutput();
                reachabilitiesBuilder.addAll(transitRides);

                if (!newTablePreviousRowRecord.getMode().getType()
                        .equals(ModeType.WALKING)) {
                    final FlatWalkVisitor walkVisitor = new FlatWalkVisitor(
                            cutoffTime, newTableReachTime, reachabilityClient,
                            timeTracker);
                    newTablePreviousRowLocation.accept(walkVisitor);
                    final Set<ReachabilityOutput> walks
                            = walkVisitor.getOutput();
                    reachabilitiesBuilder.addAll(walks);
                }
                reachabilities = reachabilitiesBuilder.build();
            }

            for (final ReachabilityOutput reachability : reachabilities) {
                final VisitableLocation newLocation
                        = reachability.getLocation();
                final LocalDateTime newTime = reachability.getReachTime();
                final ModeInfo mode = reachability.getModeInfo();

                final DynamicProgrammingRecord record
                        = new DynamicProgrammingRecord(
                                newTime, mode, newTablePreviousRowLocation);
                replacements += replaceIfBetter(newTableCurrentRow, newLocation,
                                                record, cutoffTime,
                                                timeTracker);
            }

        }
        return replacements;
    }

    private int mergeRowFromPriorTableRow(
            final Map<VisitableLocation, DynamicProgrammingRecord> priorTableCurrentRow,
            final Map<VisitableLocation, DynamicProgrammingRecord> priorTablePreviousRow,
            final Map<VisitableLocation, DynamicProgrammingRecord> newTableCurrentRow,
            final Map<VisitableLocation, DynamicProgrammingRecord> newTablePreviousRow,
            final LocalDateTime cutoffTime, final TimeTracker timeTracker) {
        int replacements = 0;
        for (final VisitableLocation priorTableCurrentLocation
                     : priorTableCurrentRow.keySet()) {
            final DynamicProgrammingRecord priorTableCurrentRecord
                    = priorTableCurrentRow.get(priorTableCurrentLocation);

            final VisitableLocation predecessorLocation
                    = priorTableCurrentRecord.getPredecessor();
            /* If the predecessor is null, this is the starting point. It should
               already be in place by bringing down the previous row. Thus it
               can be skippid in this path. */
            if (predecessorLocation != null) {

                /* The new table will not contain the predecessor if the 
                   predecessor is beyond the new cutoff time. Do not copy this 
                   path. */
                if (newTablePreviousRow.containsKey(predecessorLocation)) {
                    final DynamicProgrammingRecord newTablePredecessorRecord
                            = newTablePreviousRow.get(predecessorLocation);
                    final LocalDateTime newTablePredecessorReachTime
                            = newTablePredecessorRecord.getReachTime();

                    final ModeInfo mode = priorTableCurrentRecord.getMode();
                    final ModeType type = mode.getType();
                    if (type.equals(ModeType.WALKING)) {

                        /* Do not copy the path if it would create a walk to a
                           walk. It will just be replaced by a direct walk.
                         */
                        if (!newTablePredecessorRecord.getMode().getType()
                                .equals(ModeType.WALKING)) {

                            // Adjust walk for changed predecessor arrival time.
                            final DynamicProgrammingRecord priorTablePredecessorRecord
                                    = priorTablePreviousRow.get(
                                            predecessorLocation);
                            final LocalDateTime priorTableReachTime
                                    = priorTableCurrentRecord.getReachTime();
                            final LocalDateTime priorTablePredecessorReachTime
                                    = priorTablePredecessorRecord.getReachTime();
                            final Duration walkDuration = timeTracker
                                    .getDuration(
                                            priorTablePredecessorReachTime,
                                            priorTableReachTime);

                            final LocalDateTime reachTime = timeTracker.adjust(
                                    newTablePredecessorReachTime,
                                    walkDuration);
                            log.debug(
                                    "Adjusting walk for {} time to {} as predecessor arrival time has changed from {} to {}.",
                                    priorTableCurrentLocation, reachTime,
                                    priorTablePredecessorReachTime,
                                    newTablePredecessorReachTime);
                            final DynamicProgrammingRecord newTableMergedRecord
                                    = new DynamicProgrammingRecord(
                                            reachTime, mode,
                                            priorTableCurrentRecord
                                                    .getPredecessor());
                            replacements += replaceIfBetter(
                                    newTableCurrentRow,
                                    priorTableCurrentLocation,
                                    newTableMergedRecord, cutoffTime,
                                    timeTracker);
                        }
                    } else if (type.equals(ModeType.NONE)) {
                        // Adjust to be predecessor arrival time.
                        final LocalDateTime reachTime
                                = newTablePredecessorReachTime;
                        final DynamicProgrammingRecord newTableMergedRecord
                                = new DynamicProgrammingRecord(
                                        reachTime, mode,
                                        priorTableCurrentRecord.getPredecessor());
                        replacements += replaceIfBetter(
                                newTableCurrentRow, priorTableCurrentLocation,
                                newTableMergedRecord, cutoffTime, timeTracker);
                    } else {
                        /* No adjustment, as arrival does not depend on 
                           predecessor time. */
                        final LocalDateTime reachTime
                                = priorTableCurrentRecord.getReachTime();
                        final DynamicProgrammingRecord newTableMergedRecord
                                = new DynamicProgrammingRecord(
                                        reachTime, mode,
                                        priorTableCurrentRecord.getPredecessor());
                        replacements += replaceIfBetter(
                                newTableCurrentRow, priorTableCurrentLocation,
                                newTableMergedRecord, cutoffTime, timeTracker);
                    }
                }
            }
        }
        return replacements;
    }

    private static int replaceIfBetter(
            final Map<VisitableLocation, DynamicProgrammingRecord> newTableCurrentRow,
            final VisitableLocation location,
            final DynamicProgrammingRecord newRecord,
            final LocalDateTime cutoffTime,
            final TimeTracker timeTracker) {
        int replacements = 0;
        final LocalDateTime newTime = newRecord.getReachTime();
        if (timeTracker.meetsCutoff(newTime, cutoffTime)) {
            if (newTableCurrentRow.containsKey(location)) {
                final DynamicProgrammingRecord currentNewRecord
                        = newTableCurrentRow.get(location);
                if (timeTracker.shouldReplace(currentNewRecord.getReachTime(),
                                              newTime)) {
                    newTableCurrentRow.put(location, newRecord);
                    replacements++;
                }
            } else {
                newTableCurrentRow.put(location, newRecord);
                replacements++;
            }
        }
        return replacements;
    }

    private void updateScoreCard(
            final Table<Integer, VisitableLocation, DynamicProgrammingRecord> table,
            final TaskIdentifier task, final ScoreCard scoreCard,
            final MovementAssembler assembler)
            throws InterruptedException {
        final Map<VisitableLocation, DynamicProgrammingRecord> lastRow
                = table.row(table.rowKeySet().size() - 1);
        final Set<VisitableLocation> reachedLocations = lastRow.keySet();

        for (final VisitableLocation reachedLocation : reachedLocations) {

            final MovementPath path = assembler.assemble(reachedLocation,
                                                         lastRow);
            scoreCard.putPath(reachedLocation, task, path);
        }
    }

}
