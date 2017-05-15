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

import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.visitors.VisitorFactory;
import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.visitors.Visitation;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Workflows for solving reachability problems.
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class Workflow {

    private final TimeTracker timeAdjuster;
    private final MovementPath basePath;
    private final ForkJoinPool pool;
    private final WorkAllocator workAllocator;

    public void getPathsForTasks(
            final Duration duration, final Set<VisitorFactory> visitorFactories,
            final Set<TaskIdentifier> tasks)
            throws InterruptedException, ExecutionException {

        final ForkJoinTask task = new RecursiveAction() {
            @Override
            protected void compute() {

                final ImmutableList.Builder<Visitation> visitationsBuilder
                        = ImmutableList.builder();
                for (final TaskIdentifier taskId : tasks) {
                    final LocalDateTime startTime = taskId.getTime();
                    final LocalDateTime cutoffTime
                            = timeAdjuster.adjust(startTime, duration);
                    for (final VisitorFactory visitorFactory
                                 : visitorFactories) {
                        final Visitor visitor = visitorFactory.getVisitor(
                                taskId, cutoffTime, startTime, Mode.NONE, null,
                                basePath, 0, visitorFactories);
                        visitationsBuilder.add(new Visitation(
                                taskId.getCenter(), visitor));
                    }
                }
                try {
                    workAllocator.work(visitationsBuilder.build());
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        pool.execute(task);
        task.join();
    }
}
