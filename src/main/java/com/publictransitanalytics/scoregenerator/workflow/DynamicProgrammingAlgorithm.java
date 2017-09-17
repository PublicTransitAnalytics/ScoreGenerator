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

/**
 * Dynamic programming algorithm for path finding.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public final class DynamicProgrammingAlgorithm {

    public static Table<Integer, VisitableLocation, DynamicProgrammingRecord>
            createTable(final LocalDateTime startTime,
                        final LocalDateTime cutoffTime,
                        final VisitableLocation startLocation,
                        final TimeTracker timeTracker,
                        final Duration duration, final int depth,
                        final ReachabilityClient reachabilityClient,
                        final RiderFactory riderFactory) throws
            InterruptedException {
        final Table<Integer, VisitableLocation, DynamicProgrammingRecord> stateTable
                = HashBasedTable.create();

        final DynamicProgrammingRecord initialRecord
                = new DynamicProgrammingRecord(
                        startTime, ModeInfo.NONE, null);

        stateTable.put(0, startLocation, initialRecord);

        for (int i = 1; i < depth; i++) {
            final Map<VisitableLocation, DynamicProgrammingRecord> priorRow
                    = stateTable.row(i - 1);
            final Map<VisitableLocation, DynamicProgrammingRecord> currentRow
                    = stateTable.row(i);
            currentRow.putAll(priorRow);

            int roundUpdates = 0;
            for (final VisitableLocation priorLocation : priorRow.keySet()) {
                final DynamicProgrammingRecord priorRecord
                        = priorRow.get(priorLocation);
                final LocalDateTime currentTime = priorRecord
                        .getReachTime();

                final FlatWalkVisitor walkVisitor = new FlatWalkVisitor(
                        cutoffTime, currentTime, reachabilityClient,
                        timeTracker);
                final FlatTransitRideVisitor transitRideVisitor
                        = new FlatTransitRideVisitor(cutoffTime, cutoffTime,
                                                     currentTime, riderFactory);
                final ImmutableSet.Builder<ReachabilityOutput> reachabilitiesBuilder
                        = ImmutableSet.builder();
                if (!priorRecord.getMode().getType().equals(ModeType.WALKING)) {
                    priorLocation.accept(walkVisitor);
                    final Set<ReachabilityOutput> walks = walkVisitor
                            .getOutput();
                    reachabilitiesBuilder.addAll(walks);
                }
                priorLocation.accept(transitRideVisitor);
                final Set<ReachabilityOutput> transitRides
                        = transitRideVisitor.getOutput();
                reachabilitiesBuilder.addAll(transitRides);

                final int updates = updateRow(reachabilitiesBuilder.build(),
                                              currentRow, priorLocation);
                roundUpdates += updates;
            }

            if (roundUpdates == 0) {
                log.debug(
                        "Stopped processing at row {} because no updates.",
                        i);
                break;
            }

        }
        return stateTable;
    }

    private static int updateRow(
            final Set<ReachabilityOutput> reachabilities,
            final Map<VisitableLocation, DynamicProgrammingRecord> currentRow,
            final VisitableLocation priorLocation) {
        int updates = 0;
        for (final ReachabilityOutput reachability : reachabilities) {
            final VisitableLocation newLocation = reachability
                    .getLocation();
            final LocalDateTime newTime = reachability.getReachTime();

            final ModeInfo mode = reachability.getModeInfo();

            final DynamicProgrammingRecord record
                    = new DynamicProgrammingRecord(
                            newTime, mode, priorLocation);

            if (currentRow.containsKey(newLocation)) {
                final DynamicProgrammingRecord earlierThisRoundReach
                        = currentRow.get(newLocation);
                if (record.compareTo(earlierThisRoundReach) < 0) {
                    currentRow.put(newLocation, record);
                    updates++;
                }
            } else {
                currentRow.put(newLocation, record);
                updates++;
            }
        }
        return updates;
    }

    private DynamicProgrammingAlgorithm() {
    }
}
