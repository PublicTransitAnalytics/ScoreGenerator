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
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.visitors.VisitAction;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import com.publictransitanalytics.scoregenerator.visitors.VisitorFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.ForkJoinPool;

/**
 * Workflows for solving reachability problems.
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class Workflow {

    private final ImmutableSet<PointLocation> validPoints;
    private final ImmutableSet<Sector> validSectors;
    private final ForkJoinPool pool;
    private final TimeTracker timeAdjuster;
    private final MovementPath basePath;
    private final Set<VisitorFactory> visitorFactories;

    public Multiset<PointLocation> getReachedPointsOverRange(
            final PointLocation startLocation, final Duration duration,
            final LocalDateTime rangeStartTime,
            final LocalDateTime rangeEndTime, final Duration interval)
            throws InterruptedException, ExecutionException {
        final CompletionService<LocalDateTime> completionService
                = new ExecutorCompletionService<>(pool);

        int periods = 0;
        LocalDateTime time = rangeStartTime;
        while (time.isBefore(rangeEndTime)) {
            completionService.submit(createPointAndRangeCall(
                    duration, time, startLocation));
            time = time.plus(interval);
            periods++;
        }

        final Multiset<PointLocation> pointFrequencyMap = HashMultiset.create();

        for (int i = 0; i < periods; i++) {
            LocalDateTime completedTime = completionService.take().get();
            final Multimap<PointLocation, MovementPath> pointPaths
                    = extractPointPaths(completedTime);

            final Set<PointLocation> locations = pointPaths.keySet();
            for (final PointLocation location : locations) {
                pointFrequencyMap.count(location);
            }
        }
        return pointFrequencyMap;
    }

    public Multiset<Sector> getReachedSectorsOverRange(
            final PointLocation startLocation, final Duration duration,
            final LocalDateTime rangeStartTime,
            final LocalDateTime rangeEndTime, final Duration interval)
            throws InterruptedException, ExecutionException {
        final CompletionService<LocalDateTime> completionService
                = new ExecutorCompletionService<>(pool);

        int periods = 0;
        LocalDateTime time = rangeStartTime;
        while (time.isBefore(rangeEndTime)) {
            completionService.submit(createPointAndRangeCall(
                    duration, time, startLocation));
            time = time.plus(interval);
            periods++;
        }

        final Multiset<Sector> sectorFrequencyMap = HashMultiset.create();

        for (int i = 0; i < periods; i++) {
            LocalDateTime completedTime = completionService.take().get();
            final Multimap<Sector, MovementPath> sectorPaths
                    = extractSectorPaths(completedTime);

            final Set<Sector> locations = sectorPaths.keySet();
            for (final Sector location : locations) {
                sectorFrequencyMap.count(location);
            }
        }
        return sectorFrequencyMap;
    }

    public Multimap<PointLocation, MovementPath> getLocationPathsAtTime(
            final Duration tripDuration, final LocalDateTime startDateTime,
            final PointLocation startLocation) throws
            ExecutionException, InterruptedException {

        pool.submit(createPointAndRangeCall(tripDuration, startDateTime,
                                                startLocation)).get();
        return extractPointPaths(startDateTime);
    }

    public Multimap<Sector, MovementPath> getSectorPathsAtTime(
            final Duration tripDuration, final LocalDateTime startDateTime,
            final PointLocation startLocation) throws
            ExecutionException, InterruptedException {

        pool.submit(createPointAndRangeCall(tripDuration, startDateTime,
                                                startLocation)).get();
        return extractSectorPaths(startDateTime);
    }

    private Callable<LocalDateTime> createPointAndRangeCall(
            final Duration tripDuration, final LocalDateTime startDateTime,
            final PointLocation startLocation) {
        return new Callable<LocalDateTime>() {

            @Override
            public LocalDateTime call() throws Exception {
                solveProblem(tripDuration, startDateTime, startLocation);
                return startDateTime;
            }
        };
    }

    private void solveProblem(final Duration tripDuration,
                              final LocalDateTime startTime,
                              final PointLocation startLocation)
            throws InterruptedException {
        final LocalDateTime cutoffTime = timeAdjuster.adjust(startTime,
                                                             tripDuration);
        final ImmutableList.Builder<VisitAction> visitActionsBuilder = 
                ImmutableList.builder();
        for (final VisitorFactory visitorFactory : visitorFactories) {
            final Visitor visitor = visitorFactory.getVisitor(
                    startTime, cutoffTime, startTime, Mode.NONE, null,
                    basePath, 0, visitorFactories);
            final VisitAction visitAction 
                    = new VisitAction(startLocation, visitor);
            visitActionsBuilder.add(visitAction);
            pool.invoke(visitAction);
        }
        for (final VisitAction visitAction : visitActionsBuilder.build()) {
            visitAction.join();
        }

    }

    private Multimap<PointLocation, MovementPath> extractPointPaths(
            final LocalDateTime startDateTime) {
        ImmutableMultimap.Builder<PointLocation, MovementPath> builder
                = ImmutableMultimap.builder();

        for (final PointLocation stop : validPoints) {
            if (stop.getPaths().containsKey(startDateTime)) {
                for (final MovementPath path : stop.getPaths()
                        .get(startDateTime)) {
                    builder.put(stop, path);
                }
            }
        }
        return builder.build();
    }

    private Multimap<Sector, MovementPath> extractSectorPaths(
            final LocalDateTime startDateTime) {
        final ImmutableMultimap.Builder builder = ImmutableMultimap.builder();

        for (final Sector sector : validSectors) {
            if (sector.getPaths().containsKey(startDateTime)) {
                for (final MovementPath path : sector.getPaths().get(
                        startDateTime)) {
                    builder.put(sector, path);
                }
            }
        }
        return builder.build();
    }
}
