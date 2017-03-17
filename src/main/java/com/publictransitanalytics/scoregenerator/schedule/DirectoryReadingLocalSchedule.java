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

import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.TripDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * A transit schedule as viewed from the context of a transit stop backed by
 * directories.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class DirectoryReadingLocalSchedule implements LocalSchedule {

    final TransitStop associatedStop;
    final NavigableMap<LocalDateTime, Trip> timeTripMap;

    public DirectoryReadingLocalSchedule(
            final TransitStop associatedStop, final LocalDateTime earliestTime,
            final LocalDateTime latestTime, 
            final StopTimesDirectory stopTimesDirectory,
            final RouteDetailsDirectory routeDetailsDirectory,
            final ServiceTypeCalendar serviceTypeCalendar,
            final TripDetailsDirectory tripDetailsDirectory,
            final Map<String, TransitStop> stopIdMap) {

        this.associatedStop = associatedStop;
        timeTripMap = new TreeMap<>();

        final TransitTime earliestTransitTime = TransitTime.fromLocalTime(
                earliestTime.toLocalTime());
        final TransitTime latestTransitTime = TransitTime.fromLocalTime(
                latestTime.toLocalTime());
        final ServiceSet serviceSet = serviceTypeCalendar.getServiceType(
                earliestTime.toLocalDate());

        createTrips(earliestTransitTime, latestTransitTime, serviceSet,
                    earliestTime, latestTime, routeDetailsDirectory,
                    stopTimesDirectory, tripDetailsDirectory, stopIdMap);

        final TransitTime earliestOverflowedTransitTime = TransitTime
                .fromLocalTimeWithOverflow(earliestTime.toLocalTime());
        final TransitTime latestOverflowedTransitTime
                = TransitTime
                .fromLocalTimeWithOverflow(latestTime.toLocalTime());
        final ServiceSet overflowedServiceSet = serviceTypeCalendar
                .getServiceType(earliestTime.toLocalDate().minusDays(1));

        createTrips(earliestOverflowedTransitTime, latestOverflowedTransitTime,
                    overflowedServiceSet, earliestTime, latestTime,
                    routeDetailsDirectory, stopTimesDirectory,
                    tripDetailsDirectory, stopIdMap);

    }

    final void createTrips(final TransitTime startTransitTime,
                           final TransitTime endTransitTime,
                           final ServiceSet serviceSet, LocalDateTime startTime,
                           final LocalDateTime endTime,
                           final RouteDetailsDirectory routeDetailsDirectory,
                           final StopTimesDirectory stopTimesDirectory,
                           final TripDetailsDirectory tripDetailsDirectory,
                           final Map<String, TransitStop> stopIdMap) {
        log.info(String.format("Creating trips for %s %s-%s", associatedStop
                               .getStopId(),
                               startTransitTime, endTransitTime));

        final List<TripStop> stopTimes = stopTimesDirectory.getStopsAtStop(
                associatedStop.getStopId(), startTransitTime, endTransitTime);

        for (final TripStop stopTime : stopTimes) {
            final com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId datalayerTripId
                    = stopTime.getTripId();
            final TripId tripId = new TripId(datalayerTripId.getRawTripId(),
                                             datalayerTripId.getQualifier());
            final TripDetails tripDetails = tripDetailsDirectory.getTripDetails(
                    new TripGroupKey(datalayerTripId.getRawTripId()));
            if (serviceSet.getServiceCodes().contains(tripDetails
                    .getServiceType())) {
                final RouteDetails routeDetails = routeDetailsDirectory
                        .getRouteDetails(tripDetails.getRouteId());

                final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                        tripId, routeDetails.getRouteName(),
                        routeDetails.getRouteNumber(), startTime, endTime,
                        stopTimesDirectory, stopIdMap);
                final LocalDateTime time = startTime.plus(
                        TransitTime.durationBetween(startTransitTime,
                                                    stopTime.getTime()));
                timeTripMap.put(time, trip);
            }
        }
    }

    @Override
    public Set<TripArrival> getArrivalsInRange(
            final LocalDateTime startTime, final LocalDateTime endTime) {
        return timeTripMap.subMap(startTime, true, endTime, true).entrySet()
                .stream()
                .map(entry -> new TripArrival(entry.getValue(), entry.getKey()))
                .collect(Collectors.toSet());
    }
}
