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

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.Rider;
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.rider.ScheduleReader;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;

/**
 * Visitor for transit rides that does not recurse.
 * 
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class FlatTransitRideVisitor
        implements FlatVisitor<Set<ReachabilityOutput>> {

    private final LocalDateTime cutoffTime;
    private final LocalDateTime waitCutoffTime;
    private final LocalDateTime currentTime;
    private final RiderFactory riderFactory;

    private Set<ReachabilityOutput> output;

    @Override
    public Set<ReachabilityOutput> getOutput() {
        return output;
    }

    @Override
    public void visit(Sector sector) throws InterruptedException {
        output = Collections.emptySet();
    }

    @Override
    public void visit(TransitStop transitStop) throws InterruptedException {
        final ImmutableSet.Builder<ReachabilityOutput> outputBuilder
                = ImmutableSet.builder();

        final Sector sector = transitStop.getContainingSector();
        outputBuilder.add(new ReachabilityOutput(
                sector, currentTime, currentTime, ModeInfo.NONE));

        final ScheduleReader reader = riderFactory.getScheduleReader();
        final Set<EntryPoint> entryPoints
                = reader.getEntryPoints(transitStop, currentTime,
                                        waitCutoffTime);

        for (final EntryPoint entryPoint : entryPoints) {

            final Trip trip = entryPoint.getTrip();
            final LocalDateTime entryTime = entryPoint.getTime();
            final Rider rider = riderFactory.getNewRider(
                    transitStop, entryTime, cutoffTime, trip);
            while (rider.canContinueTrip()) {

                final RiderStatus status = rider.continueTrip();

                final LocalDateTime newTime = status.getTime();
                final TransitStop newStop = status.getStop();

                outputBuilder.add(new ReachabilityOutput(
                        newStop, newTime, entryTime, 
                        new ModeInfo(ModeType.TRANSIT, entryPoint, null)));
            }
        }
        output = outputBuilder.build();
    }

    @Override
    public void visit(Landmark point) throws InterruptedException {
        output = Collections.emptySet();
    }

}
