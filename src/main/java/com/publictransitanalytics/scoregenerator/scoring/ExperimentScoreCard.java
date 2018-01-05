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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRecord;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.util.Map;

/**
 * ScoreCard that identifies which experiments reached Sectors.
 *
 * @author Public Transit Analytics
 */
public class ExperimentScoreCard extends ScoreCard {

    private final Multimap<Sector, TaskIdentifier> reachedTasks;
    private final SetMultimap<PointLocation, Sector> pointSectorMap;

    public ExperimentScoreCard(
            final int taskCount,
            final SetMultimap<PointLocation, Sector> pointSectorMap) {
        super(taskCount);
        reachedTasks = Multimaps.synchronizedMultimap(HashMultimap.create());
        this.pointSectorMap = pointSectorMap;
    }

    @Override
    public int getReachedCount(final Sector location) {
        return reachedTasks.get(location).size();
    }

    public boolean hasPath(final Sector location,
                           final TaskIdentifier task) {
        return reachedTasks.containsEntry(location, task);
    }

    @Override
    public synchronized void scoreTask(
            final TaskIdentifier task,
            final Map<PointLocation, DynamicProgrammingRecord> stateMap) {
        for (final PointLocation location : stateMap.keySet()) {
            for (final Sector sector : pointSectorMap.get(location)) {
                reachedTasks.put(sector, task);
            }
        }
    }

    @Override
    public boolean hasPath(final Sector location) {
        return reachedTasks.containsKey(location);
    }

}
