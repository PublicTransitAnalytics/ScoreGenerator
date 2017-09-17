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
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;

/**
 *  ScoreCard that identifies which experiments reached Sectors.
 * 
 * @author Public Transit Analytics
 */
public class ExperimentScoreCard extends ScoreCard {

    final Multimap<VisitableLocation, TaskIdentifier> reachedTasks;

    public ExperimentScoreCard(final int taskCount) {
        super(taskCount);
        reachedTasks = Multimaps.synchronizedMultimap(HashMultimap.create());
    }

    @Override
    public int getReachedCount(final VisitableLocation location) {
        return reachedTasks.get(location).size();
    }

    public boolean hasPath(final VisitableLocation location,
                           final TaskIdentifier task) {
        return reachedTasks.containsEntry(location, task);
    }

    @Override
    public void putPath(VisitableLocation location, TaskIdentifier task,
                        MovementPath path) {
        reachedTasks.put(location, task);
    }

    @Override
    public boolean hasPath(final VisitableLocation location) {
        return reachedTasks.containsKey(location);
    }

}
