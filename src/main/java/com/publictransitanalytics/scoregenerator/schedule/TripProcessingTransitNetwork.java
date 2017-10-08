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

    final TreeBasedTable<TransitStop, LocalDateTime, EntryPoint> entryPoints;
    @Getter
    final Set<Trip> trips;

    public TripProcessingTransitNetwork(final TripCreator tripCreator)
            throws InterruptedException {
        
        trips = tripCreator.createTrips();
        entryPoints = NetworkHelpers.makeEntyPointTable(trips);
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

    @Override
    public Duration getInServiceTime() {
        Duration duration = Duration.ZERO;
        for (final Trip trip : trips) {
            duration = duration.plus(trip.getInServiceTime());
        }
        return duration;
    }

}
