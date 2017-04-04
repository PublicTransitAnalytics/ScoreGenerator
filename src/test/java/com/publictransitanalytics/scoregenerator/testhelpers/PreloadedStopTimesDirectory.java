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
package com.publictransitanalytics.scoregenerator.testhelpers;

import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
public class PreloadedStopTimesDirectory implements StopTimesDirectory {

    private final TreeBasedTable<TripId, TransitTime, TripStop> tripTable;
    private final TreeBasedTable<String, TransitTime, TripStop> stopTable;

    public PreloadedStopTimesDirectory(final Set<TripStop> tripStops) {
        tripTable = TreeBasedTable.create(
                (TripId a, TripId b) -> a.toString().compareTo(b.toString()),
                (TransitTime a, TransitTime b) -> a.compareTo(b));
        stopTable = TreeBasedTable.create();
        for (final TripStop tripStop : tripStops) {
            tripTable.put(tripStop.getTripId(), tripStop.getTime(), tripStop);
            stopTable.put(tripStop.getStopId(), tripStop.getTime(), tripStop);
        }
    }

    @Override
    public List<TripStop> getStopsOnTripInRange(final TripId tripId,
                                                final TransitTime startTime,
                                                final TransitTime endTime) {

        final ImmutableList.Builder<TripStop> builder = ImmutableList.builder();
        builder.addAll(tripTable.row(tripId).subMap(
                startTime, endTime).values());
        if (tripTable.contains(tripId, endTime)) {
            builder.add(tripTable.get(tripId, endTime));
        }
        return builder.build();
    }

    @Override
    public List<TripStop> getSubsequentStopsOnTrip(
            final TripId tripId, final TransitTime startTime) {
        return ImmutableList.copyOf(
                tripTable.row(tripId).tailMap(startTime).values());
    }

    @Override
    public List<TripStop> getStopsAtStopInRange(final String stopId,
                                                final TransitTime startTime,
                                                final TransitTime endTime) {

        final ImmutableList.Builder<TripStop> builder = ImmutableList.builder();
        builder.addAll(stopTable.row(stopId).subMap(
                startTime, endTime).values());
        if (stopTable.contains(stopId, endTime)) {
            builder.add(tripTable.get(stopId, endTime));
        }
        return builder.build();
    }

    @Override
    public Set<TripId> getTripIds() {
        return tripTable.rowKeySet();
    }

}
