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
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;

/**
 * A table that maps every time and place to entries into all the transit trips
 * that exist.
 *
 * @author Public Transit Analytics
 */
public class TripProcessingTransitNetwork implements TransitNetwork {

    final TreeBasedTable<TransitStop, EntryPointTimeKey, EntryPoint> entryPoints;
    @Getter
    final Set<Trip> trips;

    public TripProcessingTransitNetwork(final TripCreator tripCreator)
            throws InterruptedException {

        trips = tripCreator.createTrips();
        entryPoints = makeEntyPointTable(trips);
    }

    @Override
    public Set<EntryPoint> getEntryPoints(
            final TransitStop stop, final LocalDateTime startTime,
            final LocalDateTime endTime) {
        final ImmutableSet.Builder<EntryPoint> builder = ImmutableSet.builder();

        final EntryPointTimeKey startKey
                = EntryPointTimeKey.getMinimalKey(startTime);
        final EntryPointTimeKey endKey = EntryPointTimeKey
                .getMaximalKey(endTime);
        builder.addAll(entryPoints.row(stop).subMap(startKey, endKey).values());
        if (entryPoints.contains(stop, endKey)) {
            builder.add(entryPoints.get(stop, endKey));
        }
        return builder.build();
    }
    
    @Override
    public Set<EntryPoint> getEntryPoints(final TransitStop stop) {
        return ImmutableSet.copyOf(entryPoints.row(stop).values());
    }

    @Override
    public Duration getInServiceTime() {
        Duration duration = Duration.ZERO;
        for (final Trip trip : trips) {
            duration = duration.plus(trip.getInServiceTime());
        }
        return duration;
    }

    private static TreeBasedTable<TransitStop, EntryPointTimeKey, EntryPoint> makeEntyPointTable(
            final Set<Trip> trips) {
        final TreeBasedTable<TransitStop, EntryPointTimeKey, EntryPoint> entryPoints
                = TreeBasedTable.create(
                        (stop1, stop2) -> stop1.getIdentifier().compareTo(
                                stop2.getIdentifier()),
                        (time1, time2) -> time1.compareTo(time2));
        for (final Trip trip : trips) {
            for (final ScheduledLocation scheduledLocation
                         : trip.getSchedule()) {
                final LocalDateTime time = scheduledLocation.getScheduledTime();
                final EntryPointTimeKey timeKey = new EntryPointTimeKey(time);
                final TransitStop transitStop
                        = scheduledLocation.getLocation();
                final EntryPoint entryPoint = new EntryPoint(trip, time);
                entryPoints.put(transitStop, timeKey, entryPoint);
            }
        }
        return entryPoints;
    }
}
