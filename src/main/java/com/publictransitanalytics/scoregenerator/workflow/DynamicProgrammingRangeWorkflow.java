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

import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.rider.RiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A workflow for uses an initial dynamic programming table that is updated 
 * through a time range.
 * 
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicProgrammingRangeWorkflow implements Workflow {

    private final int depth;
    private final ScoreCard scoreCard;
    private final SectorTable sectorTable;
    private final TimeTracker timeTracker;
    private final ForkJoinPool pool;
    private final int taskSize;

    @Override
    public void getPathsForTasks(final Duration duration,
                                 final Set<ModeType> allowedModes,
                                 final RiderBehaviorFactory riderFactory,
                                 final ReachabilityClient reachabilityClient,
                                 final Set<TaskIdentifier> tasks) throws
            InterruptedException, ExecutionException {

        final SortedSetMultimap<TaskGroupIdentifier, LocalDateTime> timesByTask
                = Multimaps.newSortedSetMultimap(
                        new HashMap<>(), () -> {
                    return new TreeSet(Collections.reverseOrder());
                });

        for (final TaskIdentifier task : tasks) {
            final LocalDateTime time = task.getTime();
            final TaskGroupIdentifier noTime
                    = new TaskGroupIdentifier(task.getCenter(),
                                                    task.getExperimentName());
            timesByTask.put(noTime, time);
        }

        final DynamicProgrammingRangeTask completeTask
                = new DynamicProgrammingRangeTask(
                        depth, scoreCard, sectorTable, timeTracker, taskSize,
                        duration, allowedModes, riderFactory, 
                        reachabilityClient, timesByTask.keySet(), timesByTask);

        pool.execute(completeTask);
        completeTask.join();
    }
}
