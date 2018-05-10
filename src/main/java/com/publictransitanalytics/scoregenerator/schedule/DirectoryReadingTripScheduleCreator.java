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
package com.publictransitanalytics.scoregenerator.schedule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.TripDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStops;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class DirectoryReadingTripScheduleCreator {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final StopTimesDirectory stopTimesDirectory;
    private final RouteDetailsDirectory routeDetailsDirectory;
    private final TripDetailsDirectory tripDetailsDirectory;
    private final ServiceTypeCalendar calendar;
    private final Map<String, TransitStop> stopIdMap;

    public Set<TripSchedule> createTrips() throws InterruptedException {
        final List<Window> windows = getTripWindows(
                startTime, endTime);
        final ImmutableSet.Builder<TripSchedule> tripsBuilder
                = ImmutableSet.builder();
        for (final Window window : windows) {
            final TransitTime windowStart = window.getStartTime();
            final TransitTime windowEnd = window.getEndTime();
            final Set<TripStops> allStops = stopTimesDirectory.getAllTripStops(
                    windowStart, windowEnd);
            final LocalDate serviceDay = window.getDate();

            for (final TripStops tripStops : allStops) {
                final com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId datastoreTripId
                        = tripStops.getId();
                final TripId tripId = new TripId(
                        datastoreTripId.getRawTripId(), serviceDay,
                        datastoreTripId.getQualifier());

                final TripDetails tripDetails = tripDetailsDirectory
                        .getTripDetails(new TripGroupKey(tripId.getBaseId()));
                final String tripServiceType = tripDetails.getServiceType();

                final RouteDetails routeDetails = routeDetailsDirectory
                        .getRouteDetails(tripDetails.getRouteId());
                if (routeDetails == null) {
                    throw new ScoreGeneratorFatalException(String.format(
                            "Trip %s provided route id %s, " +
                            "which does not have route details.",
                            tripDetails, tripDetails.getRouteId()));
                }

                final String routeName = routeDetails.getRouteName();
                final String routeNumber = routeDetails.getRouteNumber();

                final ServiceSet serviceType
                        = calendar.getServiceType(serviceDay);

                if (serviceType.getServiceCodes().contains(tripServiceType)) {
                    final Set<ScheduleEntry> tripSchedule
                            = getSchedule(tripStops, serviceDay);
                    if (tripSchedule.size() > 1) {
                        final TripSchedule trip = new TripSchedule(
                                tripId, routeName, routeNumber, tripSchedule);

                        tripsBuilder.add(trip);
                    }
                }
            }
        }

        return tripsBuilder.build();
    }

    private Set<ScheduleEntry> getSchedule(final TripStops tripStops,
                                           final LocalDate serviceDay) {
        final List<TripStop> stops = tripStops.getStops();

        final ImmutableSet.Builder<ScheduleEntry> tripScheduleBuilder
                = ImmutableSet.builder();

        for (final TripStop tripStopEntry : stops) {
            final TransitTime arrivalTransitTime
                    = tripStopEntry.getArrivalTime();
            final TransitTime departureTransitTime
                    = tripStopEntry.getDepartureTime();

            final Optional<LocalDateTime> arrivalTime
                    = getStandardTime(arrivalTransitTime,
                                      serviceDay);
            final Optional<LocalDateTime> departureTime
                    = getStandardTime(departureTransitTime,
                                      serviceDay);

            final int sequence = tripStopEntry.getSequence();
            final String stopId = tripStopEntry.getStopId();
            if (stopIdMap.containsKey(stopId)) {
                /* The trip may go beyond the edges of the current
                 * map. Do not add these stops. */
                final TransitStop stop = stopIdMap.get(stopId);
                final ScheduleEntry scheduleEntry
                        = new ScheduleEntry(sequence, arrivalTime,
                                            departureTime, stop);
                tripScheduleBuilder.add(scheduleEntry);
            }
        }
        return tripScheduleBuilder.build();
    }

    private static Optional<LocalDateTime> getStandardTime(
            final TransitTime transitTime, final LocalDate serviceDay) {
        final Optional<LocalDateTime> optionalTime;
        if (transitTime == null) {
            optionalTime = Optional.empty();
        } else if (transitTime.isStandardTime()) {
            final LocalDateTime time = LocalDateTime.of(
                    serviceDay, LocalTime.of(transitTime.getHours(),
                                             transitTime.getMinutes(),
                                             transitTime.getSeconds()));
            optionalTime = Optional.of(time);
        } else {
            final LocalDateTime time = LocalDateTime.of(
                    serviceDay.plusDays(1),
                    LocalTime.of(transitTime.getHours() - 24,
                                 transitTime.getMinutes(),
                                 transitTime.getSeconds()));
            optionalTime = Optional.of(time);
        }
        return optionalTime;
    }

    private static List<Window> getTripWindows(
            final LocalDateTime earliestDateTime,
            final LocalDateTime latestDateTime) {
        final ImmutableList.Builder<Window> intervalsBuilder
                = ImmutableList.builder();

        final LocalTime earliestTime = earliestDateTime.toLocalTime();
        final LocalTime latestTime = latestDateTime.toLocalTime();
        final LocalDate earliestDate = earliestDateTime.toLocalDate();
        final LocalDate latestDate = latestDateTime.toLocalDate();

        final Period period = Period.between(earliestDate, latestDate);
        
        if (earliestDate.equals(latestDate)) {
            final Window dayWindow = new Window(
                    TransitTime.fromLocalTime(earliestTime),
                    TransitTime.fromLocalTime(latestTime), earliestDate);
            intervalsBuilder.add(dayWindow);

            final Window priorDayWindow = new Window(
                    TransitTime.fromLocalTimeWithOverflow(earliestTime),
                    TransitTime.fromLocalTimeWithOverflow(latestTime),
                    earliestDate.minusDays(1));
            intervalsBuilder.add(priorDayWindow);
        } else if (period.equals(Period.ofDays(1))) {
            final Window initialPriorDayWindow = new Window(
                    TransitTime.fromLocalTimeWithOverflow(earliestTime),
                    TransitTime.MAX_TRANSIT_TIME,
                    earliestDate.minusDays(1));
            intervalsBuilder.add(initialPriorDayWindow);

            final Window initialDayWindow = new Window(
                    TransitTime.fromLocalTime(earliestTime),
                    TransitTime.fromLocalTimeWithOverflow(latestTime),
                    earliestDate);
            intervalsBuilder.add(initialDayWindow);

            final Window nextDayWindow = new Window(
                    TransitTime.MIN_TRANSIT_TIME,
                    TransitTime.fromLocalTime(latestTime), latestDate);
            intervalsBuilder.add(nextDayWindow);
        } else if (period.equals(Period.ofDays(2))) {
            final Window initialPriorDayWindow = new Window(
                    TransitTime.fromLocalTimeWithOverflow(earliestTime),
                    TransitTime.MAX_TRANSIT_TIME,
                    earliestDate.minusDays(1));
            intervalsBuilder.add(initialPriorDayWindow);

            final Window initialDayWindow = new Window(
                    TransitTime.fromLocalTime(earliestTime),
                    TransitTime.MAX_TRANSIT_TIME, earliestDate);
            intervalsBuilder.add(initialDayWindow);

            final Window nextDayWindow = new Window(
                    TransitTime.MIN_TRANSIT_TIME,
                    TransitTime.fromLocalTimeWithOverflow(latestTime), 
                    latestDate.minusDays(1));
            intervalsBuilder.add(nextDayWindow);
            
            final Window lastDayWindow = new Window(
                    TransitTime.MIN_TRANSIT_TIME,
                    TransitTime.fromLocalTime(latestTime), 
                    latestDate);
            intervalsBuilder.add(lastDayWindow);
        } else {
            throw new ScoreGeneratorFatalException(String.format(
                    "Cannot process time range %s-%s.", earliestDateTime,
                    latestDateTime));
        }

        return intervalsBuilder.build();
    }

}
