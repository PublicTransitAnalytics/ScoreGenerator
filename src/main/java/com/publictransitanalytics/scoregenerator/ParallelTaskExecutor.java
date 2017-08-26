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
package com.publictransitanalytics.scoregenerator;

import com.google.common.collect.SortedSetMultimap;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRangeExecutor;
import com.publictransitanalytics.scoregenerator.workflow.TaskLocationGroupIdentifier;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelTaskExecutor implements LocationExecutor {

    private final DynamicProgrammingRangeExecutor timeRangeExecutor;
    private final ExecutorService pool;

    public ParallelTaskExecutor(
            final DynamicProgrammingRangeExecutor timeRangeExecutor) {

        this.timeRangeExecutor = timeRangeExecutor;
        pool = Executors.newWorkStealingPool();
    }

    @Override
    public void execute(
            SortedSetMultimap<TaskLocationGroupIdentifier, LocalDateTime> timesByTask) 
            throws InterruptedException {
        final Set<TaskLocationGroupIdentifier> tasks = timesByTask.keySet();
        for (final TaskLocationGroupIdentifier task : timesByTask.keySet()) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        timeRangeExecutor.executeTask(task,
                                                      timesByTask.get(task));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        pool.shutdown();
        while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            log.debug("Continuing to wait.");
        }
    }

}
