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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeBasedTable;
import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.TripDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A table that maps every time and place to entries into all the transit trips
 * that exist.
 *
 * @author Public Transit Analytics
 */
public class DirectoryReadingEntryPoints implements EntryPoints {

    final TreeBasedTable<TransitStop, LocalDateTime, EntryPoint> entryPoints;

    public DirectoryReadingEntryPoints(
            final LocalDateTime startTime, final LocalDateTime endTime,
            final StopTimesDirectory stopTimesDirectory,
            final RouteDetailsDirectory routeDetailsDirectory,
            final TripDetailsDirectory tripDetailsDirectory,
            final ServiceTypeCalendar calendar,
            final Map<String, TransitStop> stopIdMap) {

        entryPoints = TreeBasedTable.create(
                (stop1, stop2) -> stop1.getIdentifier().compareTo(
                        stop2.getIdentifier()),
                (time1, time2) -> time1.compareTo(time2));

        final List<IntervalOfDay> intervals = IntervalOfDay.getIntervals(
                startTime, endTime);

        final Set<com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId> tripEntries
                = stopTimesDirectory.getTripIds();
        final ImmutableSet.Builder<Trip> tripsBuilder = ImmutableSet.builder();
        for (final com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId tripEntry
             : tripEntries) {
            final TripDetails tripDetails = tripDetailsDirectory.getTripDetails(
                    new TripGroupKey(tripEntry.getRawTripId()));
            final String tripServiceType = tripDetails.getServiceType();

            final RouteDetails routeDetails = routeDetailsDirectory
                    .getRouteDetails(tripDetails.getRouteId());
            final String routeName = routeDetails.getRouteName();
            final String routeNumber = routeDetails.getRouteNumber();

            for (final IntervalOfDay interval : intervals) {
                final LocalDate serviceDay = interval.getDate();
                final LocalDateTime intervalStartTime
                        = interval.getStartTime().atDate(serviceDay);

                final ServiceSet serviceType
                        = calendar.getServiceType(serviceDay);

                if (serviceType.getServiceCodes().contains(tripServiceType)) {
                    final TripId tripId = new TripId(
                            tripEntry.getRawTripId(), serviceDay,
                            tripEntry.getQualifier());

                    final TransitTime startTransitTime = TransitTime
                            .fromLocalTime(interval.getStartTime());
                    final List<TripStop> tripStopEntries = stopTimesDirectory
                            .getSubsequentStopsOnTrip(tripEntry,
                                                      startTransitTime);

                    final ImmutableSet.Builder<ScheduleEntry> tripScheduleBuilder
                            = ImmutableSet.builder();

                    for (final TripStop tripStopEntry : tripStopEntries) {
                        final Duration timeOffset = TransitTime.durationBetween(
                                startTransitTime, tripStopEntry.getTime());
                        final LocalDateTime time
                                = intervalStartTime.plus(timeOffset);
                        if (time.isAfter(endTime)) {
                            break;
                        }

                        final int sequence = tripStopEntry.getSequence();
                        final String stopId = tripStopEntry.getStopId();
                        if (stopIdMap.containsKey(stopId)) {
                            /* The trip may go beyond the edges of the current map.
                         * Do not add these stops. */
                            final TransitStop stop = stopIdMap.get(stopId);
                            final ScheduleEntry scheduleEntry
                                    = new ScheduleEntry(
                                            sequence, time, stop);
                            tripScheduleBuilder.add(scheduleEntry);
                        }
                    }
                    final Set<ScheduleEntry> tripSchedule
                            = tripScheduleBuilder.build();
                    if (!tripSchedule.isEmpty()) {
                        final Trip trip = new Trip(
                                tripId, routeName, routeNumber, tripSchedule);
                        tripsBuilder.add(trip);
                    }
                }
            }
        }
        final ImmutableSet<Trip> trips = tripsBuilder.build();

        for (final Trip trip : trips) {
            for (final ScheduledLocation scheduledLocation
                         : trip.getCompleteSchedule()) {
                final LocalDateTime time = scheduledLocation.getScheduledTime();
                final TransitStop transitStop
                        = scheduledLocation.getLocation();
                final EntryPoint entryPoint = new EntryPoint(trip, time);
                entryPoints.put(transitStop, time, entryPoint);
            }
        }
    }

    @Override
    public Set<EntryPoint> getEntryPoints(
            final TransitStop stop, final LocalDateTime startTime,
            final LocalDateTime endTime) {
        final ImmutableSet.Builder<EntryPoint> builder = ImmutableSet.builder();

        builder.addAll(entryPoints.row(stop).subMap(startTime, endTime)
                .values());
        if (entryPoints.contains(stop, endTime)) {
            builder.add(entryPoints.get(stop, endTime));
        }
        return builder.build();
    }

}
