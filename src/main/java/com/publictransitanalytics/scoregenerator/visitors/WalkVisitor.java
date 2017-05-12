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
package com.publictransitanalytics.scoregenerator.visitors;

import com.publictransitanalytics.scoregenerator.Mode;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClientException;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.WorkAllocator;

/**
 * Finds all the locations that can be walked to given the visited point. Spawns
 * visitors to visit those locations.
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class WalkVisitor implements Visitor {

    private final TaskIdentifier task;
    private final LocalDateTime cutoffTime;
    private final LocalDateTime currentTime;
    private final Mode lastMode;
    private final TripId lastTrip;
    private final MovementPath currentPath;
    private final int currentDepth;
    private final int maxDepth;
    private final ReachabilityClient reachabilityClient;
    private final TimeTracker timeTracker;
    private final Set<VisitorFactory> visitorFactories;
    private final WorkAllocator workAllocator;

    @Override
    public void visit(final Sector sector) {
        sector.replacePath(task, currentPath);
    }

    @Override
    public void visit(final TransitStop transitStop)
            throws InterruptedException {
        walkFrom(transitStop);
    }

    @Override
    public void visit(final Landmark point) throws InterruptedException {
        walkFrom(point);
    }

    private void walkFrom(final PointLocation location)
            throws InterruptedException {
        final Sector sector = location.getContainingSector();
        sector.accept(this);

        location.replacePath(task, currentPath);

        if (currentDepth < maxDepth && !lastMode.equals(Mode.WALKING)) {

            final ImmutableList.Builder<Visitation> visitationsBuilder
                    = ImmutableList.builder();
            final Map<VisitableLocation, WalkingCosts> walkCosts
                    = getWalkCosts(location);

            for (final Entry<VisitableLocation, WalkingCosts> entry
                         : walkCosts.entrySet()) {
                final WalkingCosts costs = entry.getValue();
                final Duration walkingDuration = costs.getDuration();

                if (timeTracker.canAdjust(currentTime, walkingDuration,
                                          cutoffTime)) {
                    final LocalDateTime newTime = timeTracker.adjust(
                            currentTime, walkingDuration);
                    final VisitableLocation walkableLocation = entry.getKey();

                    final MovementPath newPath = currentPath.appendWalk(
                            location, currentTime, walkableLocation, newTime,
                            costs);

                    if (walkableLocation.hasNoBetterPath(
                            task, newPath)) {
                        for (final VisitorFactory visitorFactory
                                     : visitorFactories) {
                            final Visitor visitor = visitorFactory.getVisitor(
                                    task, cutoffTime, newTime, Mode.WALKING,
                                    lastTrip, newPath, currentDepth + 1,
                                    visitorFactories);
                            final Visitation visitation = new Visitation(
                                    walkableLocation, visitor);

                            visitationsBuilder.add(visitation);
                        }
                    }
                }
            }

            final ImmutableList<Visitation> visitations
                    = visitationsBuilder.build();
            workAllocator.work(visitations);
        }
    }

    private Map<VisitableLocation, WalkingCosts> getWalkCosts(
            final PointLocation location) throws InterruptedException {
        try {
            final Map<VisitableLocation, WalkingCosts> walkCosts
                    = reachabilityClient.getWalkingDistances(
                            location, currentTime, cutoffTime);
            return walkCosts;
        } catch (DistanceClientException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }
}
