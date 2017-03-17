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
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.Rider;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.schedule.LocalSchedule;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * A visitor that visits Locations, noting recording its presence and taking all
 * transit trips out of the location.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class TransitRideVisitor implements Visitor {

    private final LocalDateTime keyTime;
    final private LocalDateTime cutoffTime;
    private final LocalDateTime currentTime;
    private final MovementPath currentPath;
    private final TripId lastTrip;
    private final int currentDepth;
    private final int maxDepth;
    private final Map<TransitStop, LocalSchedule> scheduleBook;
    private final RiderFactory riderFactory;
    private final Set<VisitorFactory> visitorFactories;

    @Override
    public void visit(Sector sector) {
        sector.addPath(keyTime, currentPath);
    }

    @Override
    public void visit(final TransitStop transitStop)
            throws InterruptedException {
        final Sector sector = transitStop.getContainingSector();
        sector.accept(this);

        transitStop.addPath(keyTime, currentPath);

        if (currentDepth < maxDepth) {
            final LocalSchedule schedule = scheduleBook.get(transitStop);
            final Rider rider = riderFactory.getNewRider(
                    transitStop, schedule, cutoffTime);

            final Set<RiderStatus> entryPoints
                    = rider.getEntryPoints(currentTime);
            final ImmutableList.Builder<VisitAction> visitActionsBuilder
                    = ImmutableList.builder();

            for (final RiderStatus entryPoint : entryPoints) {
                /* Don't get on a trip that we came from. Either we just got off
                * of it, or we walked around a bit and came to it again.
                 */
                if (!entryPoint.getTrip().getTripId().equals(lastTrip)) {
                    rider.takeTrip(entryPoint.getTrip(), entryPoint.getTime());
                    while (rider.canContinueTrip()) {

                        final RiderStatus status = rider.continueTrip();

                        final LocalDateTime newTime = status.getTime();
                        final TransitStop newStop = status.getStop();

                        final Movement movement = rider.getRideRecord();
                        final MovementPath newPath
                                = currentPath.makeAppended(movement);

                        if (newStop.hasNoBetterPath(keyTime, newPath)) {

                            for (final VisitorFactory visitorFactory
                                         : visitorFactories) {

                                final Visitor visitor
                                        = visitorFactory.getVisitor(
                                                keyTime, cutoffTime, newTime,
                                                Mode.TRANSIT,
                                                entryPoint.getTrip()
                                                .getTripId(), newPath,
                                                currentDepth + 1,
                                                visitorFactories);
                                final VisitAction visitAction
                                        = new VisitAction(newStop, visitor);
                                visitAction.fork();
                                visitActionsBuilder.add(visitAction);
                            }
                        }
                    }
                }
            }
            for (final VisitAction action : visitActionsBuilder.build()) {
                action.join();
            }
        }
    }

    @Override
    public void visit(Landmark point) {
    }

}
