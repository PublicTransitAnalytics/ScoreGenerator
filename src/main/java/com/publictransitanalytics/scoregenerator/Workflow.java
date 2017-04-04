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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.visitors.VisitorFactory;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;

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
    private final Set<VisitorFactory> visitorFactories;

    public void getPathsOverRange(
            final PointLocation startLocation, final Duration duration,
            final LocalDateTime rangeStartTime,
            final LocalDateTime rangeEndTime, final Duration interval)
            throws InterruptedException, ExecutionException {

        LocalDateTime time = rangeStartTime;
        final ImmutableList.Builder<ReachedAction> reachedActionsBuilder
                = ImmutableList.builder();

        while (time.isBefore(rangeEndTime)) {
            final LocalDateTime cutoffTime
                    = timeAdjuster.adjust(time, duration);
            final ReachedAction action = new ReachedAction(
                    time, cutoffTime, startLocation, visitorFactories,
                    basePath);
            reachedActionsBuilder.add(action);
            action.fork();
            time = time.plus(interval);
        }

        for (final ReachedAction action : reachedActionsBuilder.build()) {
            action.join();
        }
    }

    public void getPathsAtTime(
            final Duration tripDuration, final LocalDateTime startTime,
            final PointLocation startLocation) throws
            ExecutionException, InterruptedException {

        final LocalDateTime cutoffTime
                = timeAdjuster.adjust(startTime, tripDuration);

        final ReachedAction action = new ReachedAction(
                startTime, cutoffTime, startLocation, visitorFactories,
                basePath);
        action.fork();
        action.join();
    }

}
