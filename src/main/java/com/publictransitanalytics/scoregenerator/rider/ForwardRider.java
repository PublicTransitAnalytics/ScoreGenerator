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
package com.publictransitanalytics.scoregenerator.rider;

import com.google.common.collect.PeekingIterator;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.LocalDateTime;

/**
 * A transit rider that moves forward through time, like a normal rider.
 *
 * @author Public Transit Analytics
 */
public class ForwardRider implements Rider {

    private final TransitStop initialPosition;
    private final LocalDateTime entryTime;

    private final Trip trip;
    private final LocalDateTime cutoffTime;

    private final PeekingIterator<VehicleEvent> iterator;

    public ForwardRider(final EntryPoint entryPoint,
                        final LocalDateTime cutoffTime) {
        trip = entryPoint.getTrip();
        iterator = trip.getForwardIterator(entryPoint.getSequence());
        final VehicleEvent event = trip.getSchedule().get(
                entryPoint.getSequence());

        this.initialPosition = event.getLocation();
        this.entryTime = event.getArrivalTime();
        this.cutoffTime = cutoffTime;
    }

    @Override
    public boolean canContinueTrip() {
        if (!iterator.hasNext()) {
            return false;
        }

        final VehicleEvent continued = iterator.peek();
        return !continued.getArrivalTime().isAfter(cutoffTime);
    }

    @Override
    public RiderStatus continueTrip() {
        final VehicleEvent event = iterator.next();
        return new RiderStatus(event.getLocation(), event.getArrivalTime(),
                               trip);
    }

}
