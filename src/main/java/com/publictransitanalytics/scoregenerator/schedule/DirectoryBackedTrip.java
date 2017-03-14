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

import com.bitvantage.bitvantagetypes.collections.TreeBidirectionalMap;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * The course of a single transit vehicle bound over a time interval.
 *
 * @author Public Transit Analytics
 */
public class DirectoryBackedTrip implements Trip {

    @Getter
    private final TripId tripId;
    @Getter
    private final String routeName;
    @Getter
    private final String routeNumber;

    private final TreeBidirectionalMap<TransitStop, LocalDateTime> tripSchedule;

    public DirectoryBackedTrip(final TripId tripId, final String routeName,
                               final String routeNumber,
                               final LocalDateTime earliestTime,
                               final LocalDateTime latestTime,
                               final StopTimesDirectory stopTimesDirectory,
                               final Map<String, TransitStop> stopIdMap) {

        this.tripId = tripId;
        this.routeName = routeName;
        this.routeNumber = routeNumber;

        tripSchedule = new TreeBidirectionalMap<>(
                (TransitStop o1, TransitStop o2) -> o1.getStopId().compareTo(
                        o2.getStopId()),
                (LocalDateTime o1, LocalDateTime o2) -> o1.compareTo(o2));

        final TransitTime earliestTransitTime = TransitTime.fromLocalTime(
                earliestTime.toLocalTime());
        final TransitTime latestTransitTime = TransitTime.fromLocalTime(
                latestTime.toLocalTime());
        createStopsOnTrip(stopTimesDirectory, stopIdMap, earliestTransitTime,
                          latestTransitTime, earliestTime);

        final TransitTime overflowedEarliestTransitTime = TransitTime
                .fromLocalTimeWithOverflow(earliestTime.toLocalTime());
        final TransitTime overflowedLatestTransitTime = TransitTime
                .fromLocalTimeWithOverflow(latestTime.toLocalTime());

        createStopsOnTrip(stopTimesDirectory, stopIdMap,
                          overflowedEarliestTransitTime,
                          overflowedLatestTransitTime, earliestTime);

    }

    private void createStopsOnTrip(
            final StopTimesDirectory stopTimesDirectory,
            final Map<String, TransitStop> stopIdMap,
            final TransitTime startTransitTime,
            final TransitTime endTransitTime,
            final LocalDateTime startTime) {
        final com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId directoryTripId
                = new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        tripId.getBaseId(), tripId.getOffset());

        final List<TripStop> stops = stopTimesDirectory.getStopsOnTrip(
                directoryTripId, startTransitTime, endTransitTime);

        for (final TripStop stop : stops) {
            final LocalDateTime time = startTime.plus(TransitTime
                    .durationBetween(startTransitTime, stop.getTime()));

            /* The trip may go beyond the edges of the current map or beyond
             * the allowable time. 
             * Do not add these stops. */
            if (stopIdMap.containsKey(stop.getStopId())) {
                tripSchedule.put(stopIdMap.get(stop.getStopId()), time);
            }
        }
    }

    @Override
    public ScheduledLocation getNextScheduledLocation(final TransitStop stop) {
        final LocalDateTime currentTime = tripSchedule.get(stop);

        final LocalDateTime nextTime = tripSchedule.nextValue(currentTime);
        if (nextTime == null) {
            return null;
        }
        final TransitStop nextStop = tripSchedule.getKey(nextTime);

        return new ScheduledLocation(nextStop, tripSchedule.get(nextStop));
    }

    @Override
    public ScheduledLocation
            getPreviousScheduledLocation(final TransitStop stop) {
        final LocalDateTime currentTime = tripSchedule.get(stop);

        final LocalDateTime previousTime
                = tripSchedule.previousValue(currentTime);
        if (previousTime == null) {
            return null;
        }
        final TransitStop previousStop = tripSchedule.getKey(previousTime);

        return new ScheduledLocation(previousStop,
                                     tripSchedule.get(previousStop));
    }

    @Override
    public LocalDateTime getScheduledTime(final TransitStop stop) {
        return tripSchedule.get(stop);
    }

}
