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
import com.google.common.collect.Table;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.util.Map;

/**
 * Scorecard that keeps track of beth paths.
 * 
 * @author Public Transit Analytics
 */
public class PathScoreCard extends ScoreCard {

    private final Table<VisitableLocation, TaskIdentifier, MovementPath> bestPaths;

    public PathScoreCard(final int taskCount) {
        super(taskCount);
        bestPaths = HashBasedTable.create();
    }

    public synchronized boolean hasNoBetterPath(
            final VisitableLocation location, final TaskIdentifier task,
            final MovementPath path) {
        if (!bestPaths.contains(location, task)) {
            return true;
        }
        final MovementPath bestPath = bestPaths.get(location, task);

        return path.compareTo(bestPath) < 0;
    }

    @Override
    public synchronized void putPath(final VisitableLocation location,
                                     final TaskIdentifier task,
                                     final MovementPath path) {
        bestPaths.put(location, task, path);
    }

    public synchronized MovementPath getBestPath(
            final VisitableLocation location, final TaskIdentifier task) {
        return bestPaths.get(location, task);
    }

    public synchronized Map<TaskIdentifier, MovementPath> getBestPaths(
            final VisitableLocation location) {
        return bestPaths.row(location);
    }

    @Override
    public synchronized int getReachedCount(
            final VisitableLocation location) {
        return bestPaths.row(location).size();
    }

    public boolean hasPath(final VisitableLocation location,
                           final TaskIdentifier task) {
        return bestPaths.contains(location, task);
    }

    @Override
    public boolean hasPath(final VisitableLocation location) {
        return bestPaths.containsRow(location);
    }

}
