/*
 * Copyright 2018 Public Transit Analytics.
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
package com.publictransitanalytics.scoregenerator.schedule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Creates a fully formed trip from a schedule by replacing unknown times with
 * the last known time.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class LastTimeScheduleInterpolator implements ScheduleInterpolator {

    @Override
    public Trip createTrip(final TripSchedule schedule)
            throws InterpolationException {

        final ImmutableSortedMap.Builder<Integer, VehicleEvent> sequenceBuilder
                = ImmutableSortedMap.naturalOrder();

        LocalDateTime lastArrivalTime = null;
        LocalDateTime lastDepartureTime = null;

        for (final ScheduleEntry entry : schedule.getScheduleEntries()) {
            final Optional<LocalDateTime> optionalArrivalTime
                    = entry.getArrivalTime();
            final Optional<LocalDateTime> optionalDepartureTime
                    = entry.getDepartureTime();

            final LocalDateTime arrivalTime = getOrInterpolateArrival(
                    optionalArrivalTime, optionalDepartureTime,
                    lastArrivalTime);
            final LocalDateTime departureTime = getOrInterpolateDeparture(
                    optionalDepartureTime, optionalArrivalTime,
                    lastDepartureTime);

            final VehicleEvent scheduledLocation = new VehicleEvent(
                    entry.getStop(), arrivalTime, departureTime);
            final int sequenceNumber = entry.getSequence();
            sequenceBuilder.put(sequenceNumber, scheduledLocation);
            
            lastArrivalTime = arrivalTime;
            lastDepartureTime = departureTime;
        }
        final List<VehicleEvent> sequence
                = ImmutableList.copyOf(sequenceBuilder.build().values());

        return new Trip(schedule.getTripId(), schedule.getRouteName(),
                        schedule.getRouteNumber(), sequence);

    }

    private static LocalDateTime getOrInterpolateArrival(
            final Optional<LocalDateTime> optionalArrivalTime,
            final Optional<LocalDateTime> optionalDepartureTime,
            final LocalDateTime lastArrivalTime) throws InterpolationException {
        final LocalDateTime time;
        if (optionalArrivalTime.isPresent()) {
            time = optionalArrivalTime.get();
        } else if (optionalDepartureTime.isPresent()) {
            time = optionalDepartureTime.get();
        } else {
            if (lastArrivalTime == null) {
                throw new InterpolationException(
                        "Cannot interpolate from null initial arrival time.");
            }
            time = lastArrivalTime;
        }
        return time;
    }

    private static LocalDateTime getOrInterpolateDeparture(
            final Optional<LocalDateTime> optionalDepartureTime,
            final Optional<LocalDateTime> optionalArrivalTime,
            final LocalDateTime lastDepartureTime)
            throws InterpolationException {
        final LocalDateTime time;
        if (optionalDepartureTime.isPresent()) {
            time = optionalDepartureTime.get();
        } else if (optionalArrivalTime.isPresent()) {
            time = optionalArrivalTime.get();
        } else {
            if (lastDepartureTime == null) {
                throw new InterpolationException(
                        "Cannot interpolate from null initial arrival time.");
            }
            time = lastDepartureTime;
        }
        return time;
    }

}
