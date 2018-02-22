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
 * A transit rider looks backward for how they could have gotten to a location.
 *
 * @author Public Transit Analytics
 */
public class RetrospectiveRider implements Rider {

    private final TransitStop initialPosition;
    private final LocalDateTime initialTime;

    private final Trip trip;
    private final LocalDateTime cutoffTime;

    private final PeekingIterator<VehicleEvent> iterator;

    public RetrospectiveRider(final EntryPoint entryPoint,
                              final LocalDateTime cutoffTime) {
        this.trip = entryPoint.getTrip();
        iterator = trip.getBackwardIterator(entryPoint.getSequence());
        final VehicleEvent event = trip.getSchedule().get(
                entryPoint.getSequence());

        this.initialPosition = event.getLocation();
        this.initialTime = event.getScheduledTime();
        this.cutoffTime = cutoffTime;
    }

    @Override
    public boolean canContinueTrip() {
        if (!iterator.hasNext()) {
            return false;
        }

        final VehicleEvent continued = iterator.peek();
        return !continued.getScheduledTime().isBefore(cutoffTime);
    }

    @Override
    public RiderStatus continueTrip() {
        final VehicleEvent event = iterator.next();
        return new RiderStatus(event.getLocation(), event.getScheduledTime(),
                               trip);
    }

}
