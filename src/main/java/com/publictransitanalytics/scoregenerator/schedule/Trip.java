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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private final ImmutableList<VehicleEvent> sequence;

    public Trip(final TripId tripId, final String routeName,
                final String routeNumber, final Set<ScheduleEntry> stops,
                final ScheduleInterpolator interpolator) {
        this(tripId, routeName, routeNumber, getSequence(stops, interpolator));
    }

    private static List<VehicleEvent> getSequence(
            final Set<ScheduleEntry> stops, 
            final ScheduleInterpolator interpolator) {
        final ImmutableSortedMap.Builder<Integer, VehicleEvent> sequenceBuilder
                = ImmutableSortedMap.naturalOrder();

        for (final ScheduleEntry entry : stops) {
            final Optional<LocalDateTime> optionalTime = entry.getTime();
            final LocalDateTime time;
            if (optionalTime.isPresent()) {
                time = optionalTime.get();
                interpolator.setBaseTime(time);
            } else {
                time = interpolator.getInterpolatedTime(entry.getStop());
            }

            final VehicleEvent scheduledLocation = new VehicleEvent(
                    entry.getStop(), time);
            final int sequenceNumber = entry.getSequence();
            sequenceBuilder.put(sequenceNumber, scheduledLocation);
        }
        final List<VehicleEvent> sequence
                = ImmutableList.copyOf(sequenceBuilder.build().values());
        return sequence;
    }
    
    public List<VehicleEvent> getSchedule() {
        return sequence;
    }

    public Trip(final TripId tripId, final String routeName,
                final String routeNumber,
                final List<VehicleEvent> sequence) {
        this.tripId = tripId;
        this.routeName = routeName;
        this.routeNumber = routeNumber;
        this.sequence = ImmutableList.copyOf(sequence);
    }

    public PeekingIterator<VehicleEvent> getForwardIterator(
            final int currentSequence) {
        if (currentSequence >= sequence.size() - 1) {
            return Iterators.peekingIterator(Collections.emptyIterator());
        }

        return Iterators.peekingIterator(sequence.subList(
                currentSequence + 1, sequence.size()).iterator());
    }

    public PeekingIterator<VehicleEvent> getBackwardIterator(
            final int currentSequence) {

        if (currentSequence == 0) {
            return Iterators.peekingIterator(Collections.emptyIterator());
        }

        return Iterators.peekingIterator(sequence.subList(
                0, currentSequence).reverse().iterator());
    }

    public Duration getInServiceTime() {
        final LocalDateTime startTime = sequence.get(0).getScheduledTime();
        final LocalDateTime endTime
                = sequence.get(sequence.size() - 1).getScheduledTime();
        return Duration.between(startTime, endTime);
    }

}
