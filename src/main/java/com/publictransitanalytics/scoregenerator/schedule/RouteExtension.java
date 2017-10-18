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
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class RouteExtension implements Patch {

    private final String routeNumber;
    private final TransitStop referenceStop;
    private final ExtensionType type;
    private final NavigableMap<Duration, TransitStop> extensionSequence;

    @Override
    public Optional<Trip> patch(final Trip original) {
        Trip newTrip = original;
        final List<ScheduledLocation> schedule = newTrip.getSchedule();
        if (schedule.isEmpty()) {
            log.info("Trip {} was not extended because it is empty.",
                     newTrip.getTripId());
        } else if (schedule.size() == 1) {
            final TransitStop stop = schedule.get(0).getLocation();
            if (referenceStop.equals(stop)) {
                if (type.equals(ExtensionType.AFTER_LAST)) {
                    final List<ScheduledLocation> newSchedule
                            = appendToSchedule(schedule, extensionSequence);
                    newTrip = makeReplacementTrip(newTrip, newSchedule);
                } else if (type.equals(
                        ExtensionType.BEFORE_FIRST)) {
                    final List<ScheduledLocation> newSchedule
                            = prependToSchedule(schedule, extensionSequence);
                    newTrip = makeReplacementTrip(newTrip, newSchedule);
                }
            } else {
                log.info("Refenece stop {} was not found in "
                                 + "singleton trip {}",
                         stop.getIdentifier(), newTrip.getTripId());
            }
        } else {
            if (type.equals(ExtensionType.AFTER_LAST)) {
                final TransitStop lastStop = schedule.get(
                        schedule.size() - 1).getLocation();
                if (referenceStop.equals(lastStop)) {
                    final List<ScheduledLocation> newSchedule
                            = appendToSchedule(schedule, extensionSequence);
                    newTrip = makeReplacementTrip(newTrip, newSchedule);
                }
            } else if (type.equals(ExtensionType.BEFORE_FIRST)) {
                final TransitStop firstStop
                        = schedule.get(0).getLocation();
                if (referenceStop.equals(firstStop)) {
                    final List<ScheduledLocation> newSchedule
                            = prependToSchedule(schedule, extensionSequence);
                    newTrip = makeReplacementTrip(newTrip, newSchedule);
                }
            }
        }

        return Optional.of(newTrip);
    }

    private static Trip makeReplacementTrip(
            final Trip originalTrip,
            final List<ScheduledLocation> newStops) {
        return new Trip(originalTrip.getTripId(), originalTrip.getRouteName(),
                        originalTrip.getRouteNumber(), newStops);
    }

    private static List<ScheduledLocation> appendToSchedule(
            final List<ScheduledLocation> schedule,
            final NavigableMap<Duration, TransitStop> extension) {
        final LocalDateTime baseTime
                = schedule.get(schedule.size() - 1).getScheduledTime();
        final ImmutableList.Builder<ScheduledLocation> builder
                = ImmutableList.builder();
        builder.addAll(schedule);
        for (final Duration offset : extension.keySet()) {
            builder.add(new ScheduledLocation(extension.get(offset),
                                              baseTime.plus(offset)));
        }
        return builder.build();
    }

    private static List<ScheduledLocation> prependToSchedule(
            final List<ScheduledLocation> schedule,
            final NavigableMap<Duration, TransitStop> extension) {
        final LocalDateTime baseTime = schedule.get(0).getScheduledTime();
        final ImmutableList.Builder<ScheduledLocation> builder
                = ImmutableList.builder();
        for (final Duration offset : extension.descendingKeySet()) {
            builder.add(new ScheduledLocation(extension.get(offset),
                                              baseTime.minus(offset)));
        }
        builder.addAll(schedule);
        return builder.build();
    }
}
