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
import com.google.common.collect.ImmutableMap;
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

    private final NavigableMap<Integer, ScheduledLocation> sequence;
    private final Map<ScheduledLocation, Integer> index;

    public Trip(final TripId tripId, final String routeName,
                final String routeNumber, final Set<ScheduleEntry> stops) {
        this.tripId = tripId;
        this.routeName = routeName;
        this.routeNumber = routeNumber;

        final ImmutableSortedMap.Builder<Integer, ScheduledLocation> sequenceBuilder
                = ImmutableSortedMap.naturalOrder();
        final ImmutableMap.Builder<ScheduledLocation, Integer> indexBuilder
                = ImmutableMap.builder();

        for (final ScheduleEntry stop : stops) {
            final ScheduledLocation scheduledLocation = new ScheduledLocation(
                    stop.getStop(), stop.getTime());
            final int sequence = stop.getSequence();
            sequenceBuilder.put(sequence, scheduledLocation);
            indexBuilder.put(scheduledLocation, sequence);
        }
        sequence = sequenceBuilder.build();
        index = indexBuilder.build();
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
        
        final ScheduledLocation scheduledLocation 
                = new ScheduledLocation(stop, time);
        final int sequenceNumber = index.get(scheduledLocation);
        final Map.Entry<Integer, ScheduledLocation> nextEntry
                = sequence.higherEntry(sequenceNumber);
        
        return (nextEntry == null) ? null : nextEntry.getValue();
    }

    public ScheduledLocation getPreviousScheduledLocation(
            final TransitStop stop, final LocalDateTime time) {

         final ScheduledLocation scheduledLocation 
                = new ScheduledLocation(stop, time);
        final int sequenceNumber = index.get(scheduledLocation);
        final Map.Entry<Integer, ScheduledLocation> previousEntry
                = sequence.lowerEntry(sequenceNumber);
        
        return (previousEntry == null) ? null : previousEntry.getValue();
    }

    public List<ScheduledLocation> getSchedule() {
        return ImmutableList.copyOf(sequence.values());
    }

    public Duration getInServiceTime() {
        final LocalDateTime startTime 
                = sequence.firstEntry().getValue().getScheduledTime();
        final LocalDateTime endTime
                = sequence.lastEntry().getValue().getScheduledTime();
        return Duration.between(startTime, endTime);
    }

}
