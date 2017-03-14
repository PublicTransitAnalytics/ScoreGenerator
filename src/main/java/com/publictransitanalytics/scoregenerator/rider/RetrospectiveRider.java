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

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.LocalSchedule;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.TransitRideMovement;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * A transit rider looks backward for how they could have gotten to a location.
 *
 * @author Public Transit Analytics
 */
public class RetrospectiveRider implements Rider {

    private final LocalSchedule schedule;
    private final TransitStop initialPosition;
    private final LocalDateTime cutoffTime;

    private TransitStop position;
    private Trip currentTrip;

    public RetrospectiveRider(final TransitStop initialPosition,
                              final LocalSchedule schedule,
                              final LocalDateTime cutoffTime) {
        this.initialPosition = initialPosition;
        position = initialPosition;
        this.schedule = schedule;
        this.cutoffTime = cutoffTime;
    }

    @Override
    public Collection<Trip> getTrips(final LocalDateTime currentTime) {
        return schedule.getTripsInRange(cutoffTime, currentTime);
    }

    @Override
    public void takeTrip(final Trip trip) {
        currentTrip = trip;
        position = initialPosition;
    }

    @Override
    public boolean canContinueTrip() {
        final ScheduledLocation continued
                = currentTrip.getPreviousScheduledLocation(position);
        if (continued == null) {
            return false;
        }
        return !continued.getScheduledTime().isBefore(cutoffTime);
    }

    @Override
    public RiderStatus continueTrip() {
        final ScheduledLocation location = currentTrip
                .getPreviousScheduledLocation(position);
        position = location.getLocation();
        return new RiderStatus(location.getLocation(),
                               location.getScheduledTime());
    }

    @Override
    public Movement getRideRecord() {
        return new TransitRideMovement(
                currentTrip.getTripId().getBaseId(),
                currentTrip.getRouteNumber(),
                currentTrip.getRouteName(),
                position.getStopId(), position.getStopName(),
                currentTrip.getScheduledTime(position),
                initialPosition.getStopId(), initialPosition.getStopName(),
                currentTrip.getScheduledTime(initialPosition));
    }

}
