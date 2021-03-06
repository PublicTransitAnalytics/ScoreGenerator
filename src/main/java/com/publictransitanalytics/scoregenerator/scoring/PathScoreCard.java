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
package com.publictransitanalytics.scoregenerator.scoring;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRecord;
import com.publictransitanalytics.scoregenerator.workflow.MovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scorecard that keeps track of best paths.
 *
 * @author Public Transit Analytics
 */
public class PathScoreCard extends ScoreCard {

    private final Table<Sector, LogicalTask, MovementPath> bestPaths;
    private final SetMultimap<PointLocation, Sector> pointSectorMap;
    private final MovementAssembler assembler;
    private final TimeTracker timeTracker;

    public PathScoreCard(
            final int taskCount,
            final SetMultimap<PointLocation, Sector> pointSectorMap,
            final MovementAssembler assembler, final TimeTracker timeTracker) {
        super(taskCount);
        bestPaths = HashBasedTable.create();
        this.pointSectorMap = pointSectorMap;
        this.assembler = assembler;
        this.timeTracker = timeTracker;
    }

    @Override
    public synchronized void scoreTask(
            final TaskIdentifier task,
            final Map<PointLocation, DynamicProgrammingRecord> stateMap) {

        final LogicalTask logicalTask = new LogicalTask(
                task.getTime(), task.getCenter().getLogicalCenter());

        for (final Map.Entry<PointLocation, DynamicProgrammingRecord> entry
                     : stateMap.entrySet()) {
            final PointLocation location = entry.getKey();
            final Set<Sector> sectors = pointSectorMap.get(location);
            final MovementPath path
                    = assembler.assemble(location, stateMap);
            final LocalDateTime time = entry.getValue().getReachTime();

            for (final Sector sector : sectors) {
                if (bestPaths.contains(sector, logicalTask)) {
                    if (timeTracker.shouldReplace(
                            bestPaths.get(sector, logicalTask), time)) {
                        bestPaths.put(sector, logicalTask, path);
                    }
                } else {
                    bestPaths.put(sector, logicalTask, path);
                }
            }
        }

    }

    public synchronized MovementPath getBestPath(
            final Sector location, final LogicalTask task) {
        return bestPaths.get(location, task);
    }

    public synchronized Map<LogicalTask, MovementPath> getBestPaths(
            final Sector location) {
        return bestPaths.row(location);
    }

    @Override
    public synchronized int getReachedCount(final Sector location) {
        return bestPaths.row(location).size();
    }

    public synchronized Set<LocalDateTime> getReachedTimes(
            final Sector location) {
        return bestPaths.row(location).keySet().stream()
                .map(task -> task.getTime()).collect(Collectors.toSet());
    }

    public synchronized boolean hasPath(final Sector location,
                                        final TaskIdentifier task) {
        return bestPaths.contains(location, task);
    }

    @Override
    public synchronized boolean hasPath(final Sector location) {
        return bestPaths.containsRow(location);
    }

}
