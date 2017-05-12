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
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.WorkAllocator;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.rider.RiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.rider.ScheduleReader;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.Trip;

/**
 * A visitor that visits Locations, noting recording its presence and taking all
 * transit trips out of the location.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class TransitRideVisitor implements Visitor {

    private final TaskIdentifier task;
    final private LocalDateTime cutoffTime;
    private final LocalDateTime currentTime;
    private final MovementPath currentPath;
    private final TripId lastTripId;
    private final int currentDepth;
    private final int maxDepth;
    private final RiderBehaviorFactory riderFactory;
    private final Set<VisitorFactory> visitorFactories;
    private final WorkAllocator workAllocator;

    @Override
    public void visit(Sector sector) {
        sector.replacePath(task, currentPath);
    }

    @Override
    public void visit(final TransitStop transitStop)
            throws InterruptedException {
        final Sector sector = transitStop.getContainingSector();
        sector.accept(this);

        transitStop.replacePath(task, currentPath);

        if (currentDepth < maxDepth) {
            final ScheduleReader reader = riderFactory.getScheduleReader();
            final Set<EntryPoint> entryPoints = reader
                    .getEntryPoints(transitStop, currentTime, cutoffTime);

            final ImmutableList.Builder<Visitation> visitationsBuilder
                    = ImmutableList.builder();
            for (final EntryPoint entryPoint : entryPoints) {

                final Trip trip = entryPoint.getTrip();
                /* Don't get on a trip that we came from. Either we just got off
                 * of it, or we walked around a bit and came to it again.
                 */
                if (!trip.getTripId().equals(lastTripId)) {
                    final Rider rider = riderFactory.getNewRider(
                            transitStop, entryPoint.getTime(), cutoffTime,
                            entryPoint.getTrip());
                    while (rider.canContinueTrip()) {

                        final RiderStatus status = rider.continueTrip();

                        final LocalDateTime newTime = status.getTime();
                        final TransitStop newStop = status.getStop();

                        final MovementPath newPath
                                = currentPath.appendTransitRide(
                                        trip.getTripId().getBaseId(),
                                        trip.getRouteNumber(),
                                        trip.getRouteNumber(), transitStop,
                                        entryPoint.getTime(), newStop, newTime);

                        if (newStop.hasNoBetterPath(task, newPath)) {

                            for (final VisitorFactory visitorFactory
                                         : visitorFactories) {

                                final Visitor visitor
                                        = visitorFactory.getVisitor(
                                                task, cutoffTime, newTime,
                                                Mode.TRANSIT,
                                                entryPoint.getTrip()
                                                .getTripId(), newPath,
                                                currentDepth + 1,
                                                visitorFactories);
                                final Visitation visitation
                                        = new Visitation(newStop, visitor);
                                visitationsBuilder.add(visitation);
                            }
                        }
                    }
                }
            }
            
            final ImmutableList<Visitation> visitations
                    = visitationsBuilder.build();
            workAllocator.work(visitations);
        }
    }

    @Override
    public void visit(Landmark point) {
    }

}
