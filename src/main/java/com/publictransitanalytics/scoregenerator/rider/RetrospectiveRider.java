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
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
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

    private TransitStop position;
    private LocalDateTime time;

    public RetrospectiveRider(final TransitStop initialPosition,
                              final LocalDateTime initialTime,
                              final LocalDateTime cutoffTime,
                              final Trip trip) {
        this.initialPosition = initialPosition;
        this.initialTime = initialTime;
        position = initialPosition;
        time = initialTime;
        this.cutoffTime = cutoffTime;
        this.trip = trip;
    }

    @Override
    public boolean canContinueTrip() {
        final ScheduledLocation continued
                = trip.getPreviousScheduledLocation(position, time);
        if (continued == null) {
            return false;
        }
        return !continued.getScheduledTime().isBefore(cutoffTime);
    }

    @Override
    public RiderStatus continueTrip() {
        final ScheduledLocation location
                = trip.getPreviousScheduledLocation(position, time);
        position = location.getLocation();
        time = location.getScheduledTime();
        return new RiderStatus(location.getLocation(),
                               location.getScheduledTime(), trip);
    }

}
