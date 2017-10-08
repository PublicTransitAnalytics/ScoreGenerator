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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * The course of a single transit vehicle bound over a time interval.
 *
 * @author Public Transit Analytics
 */
public class Trip {

    @Getter
    private final TripId tripId;
    @Getter
    private final String routeName;
    @Getter
    private final String routeNumber;

    private final NavigableMap<Integer, TransitStop> tripSequence;
    private final BiMap<LocalDateTime, Integer> timeSequence;

    public Trip(final TripId tripId, final String routeName,
                final String routeNumber, final Set<ScheduleEntry> stops) {
        this.tripId = tripId;
        this.routeName = routeName;
        this.routeNumber = routeNumber;

        final ImmutableSortedMap.Builder<Integer, TransitStop> tripSequenceBuilder
                = ImmutableSortedMap.naturalOrder();

        ImmutableBiMap.Builder<LocalDateTime, Integer> timeSequenceBuilder
                = ImmutableBiMap.builder();
        for (final ScheduleEntry stop : stops) {
            tripSequenceBuilder.put(stop.getSequence(), stop.getStop());
            timeSequenceBuilder.put(stop.getTime(), stop.getSequence());
        }
        tripSequence = tripSequenceBuilder.build();
        timeSequence = timeSequenceBuilder.build();
    }

    public Trip(final TripId tripId, final String routeName,
                final String routeNumber, 
                final List<ScheduledLocation> orderedStops) {
        this(tripId, routeName, routeNumber, makeEntries(orderedStops));
    }
    
    private static Set<ScheduleEntry> makeEntries(
            final List<ScheduledLocation> orderedStops) {
        final ImmutableSet.Builder<ScheduleEntry> builder 
                = ImmutableSet.builder();
        for (int i = 0; i < orderedStops.size(); i++) {
            final ScheduledLocation stop = orderedStops.get(i);
            final ScheduleEntry entry = new ScheduleEntry(
                    i, stop.getScheduledTime(), stop.getLocation());
            builder.add(entry);
        }
        return builder.build();
    }

    public ScheduledLocation getNextScheduledLocation(
            final TransitStop stop, final LocalDateTime time) {

        final int sequence = timeSequence.get(time);
        final Map.Entry<Integer, TransitStop> higherEntry
                = tripSequence.higherEntry(sequence);

        if (higherEntry == null) {
            return null;
        }
        final TransitStop nextStop = higherEntry.getValue();
        final LocalDateTime nextTime
                = timeSequence.inverse().get(higherEntry.getKey());

        return new ScheduledLocation(nextStop, nextTime);
    }

    public ScheduledLocation getPreviousScheduledLocation(
            final TransitStop stop, final LocalDateTime time) {

        final int sequence = timeSequence.get(time);
        final Map.Entry<Integer, TransitStop> lowerEntry
                = tripSequence.lowerEntry(sequence);

        if (lowerEntry == null) {
            return null;
        }
        final TransitStop nextStop = lowerEntry.getValue();
        final LocalDateTime nextTime
                = timeSequence.inverse().get(lowerEntry.getKey());

        return new ScheduledLocation(nextStop, nextTime);
    }

    public List<ScheduledLocation> getSchedule() {
        return tripSequence.keySet().stream().map(
                k -> new ScheduledLocation(tripSequence.get(k),
                                           timeSequence.inverse().get(k)))
                .collect(Collectors.toList());
    }

    public Duration getInServiceTime() {
        final LocalDateTime startTime = timeSequence.inverse().get(
                tripSequence.firstKey());
        final LocalDateTime endTime = timeSequence.inverse().get(
                tripSequence.lastKey());
        return Duration.between(startTime, endTime);
    }

}
