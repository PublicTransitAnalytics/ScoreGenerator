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
package com.publictransitanalytics.scoregenerator.schedule.patching;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class RouteReroute implements Patch {

    private final String routeNumber;
    private final TransitStop referenceStop;
    private final ReferenceDirection type;
    private final List<RouteSequenceItem> sequence;
    private final TransitStop returnStop;
    private final Duration returnDelta;

    @Override
    public Optional<Trip> patch(final Trip original) {
        final List<ScheduledLocation> schedule = original.getSchedule();
        final Trip newTrip;
        if (routeNumber.equals(original.getRouteNumber())) {
            if (schedule.isEmpty()) {
                newTrip = original;
                log.info("Trip {} was not extended because it is empty.",
                         newTrip.getTripId());
            } else if (schedule.size() == 1) {
                final TransitStop stop = schedule.get(0).getLocation();
                if (referenceStop.equals(stop)) {
                    if (type.equals(ReferenceDirection.AFTER_LAST)) {
                        final List<ScheduledLocation> newSchedule
                                = Appending.appendToSchedule(schedule,
                                                             sequence);
                        newTrip = Appending.makeReplacementTrip(original,
                                                                newSchedule);
                    } else if (type.equals(ReferenceDirection.BEFORE_FIRST)) {
                        final List<ScheduledLocation> newSchedule
                                = Appending.prependToSchedule(schedule,
                                                              sequence);
                        newTrip = Appending.makeReplacementTrip(original,
                                                                newSchedule);
                    } else {
                        newTrip = original;
                    }
                } else {
                    newTrip = original;
                    log.info(
                            "Reference stop {} was not found in singleton trip {}",
                            stop.getIdentifier(), newTrip.getTripId());
                }
            } else {
                final ImmutableList.Builder<ScheduledLocation> scheduleBuilder
                        = ImmutableList.builder();
                if (type.equals(ReferenceDirection.AFTER_LAST)) {
                    final int divergingIndex = getLocationIndex(
                            schedule, referenceStop);

                    if (divergingIndex == -1) {
                        newTrip = original;
                    } else {

                        final LocalDateTime divergingTime
                                = makeForwardPreDivergingPortion(
                                        schedule, divergingIndex,
                                        scheduleBuilder);

                        final LocalDateTime lastRerouteTime
                                = makeForwardReroute(divergingTime,
                                                     scheduleBuilder);

                        if (returnStop != null) {
                            final List<ScheduledLocation> remainder
                                    = schedule.subList(divergingIndex,
                                                       schedule.size());
                            makeForwardPostDivergingPortion(
                                    remainder, lastRerouteTime,
                                    scheduleBuilder);
                        }
                        newTrip = Appending.makeReplacementTrip(
                                original, scheduleBuilder.build());
                    }
                } else if (type.equals(ReferenceDirection.BEFORE_FIRST)) {
                    final int divergingIndex = getLastLocationIndex(
                            schedule, referenceStop);

                    if (divergingIndex == -1) {
                        newTrip = original;
                    } else {

                        final List<ScheduledLocation> end = schedule.subList(
                                divergingIndex, schedule.size());
                        final LocalDateTime divergingTime
                                = end.get(0).getScheduledTime();

                        final List<ScheduledLocation> reroute
                                = makeBackwardReroute(divergingTime);
                        final LocalDateTime lastRerouteTime
                                = reroute.get(0).getScheduledTime();

                        if (returnStop != null) {
                            final List<ScheduledLocation> remainder
                                    = schedule.subList(0, divergingIndex + 1);
                            final List<ScheduledLocation> beginning
                                    = makeBackwardPostDivergingPortion(
                                            remainder, lastRerouteTime);
                            scheduleBuilder.addAll(beginning);
                        }
                        
                        scheduleBuilder.addAll(reroute);
                        scheduleBuilder.addAll(end);

                        newTrip = Appending.makeReplacementTrip(
                                original, scheduleBuilder.build());
                    }
                } else {
                    newTrip = original;
                }
            }
        } else {
            newTrip = original;
        }

        return Optional.of(newTrip);
    }

    private void makeForwardPostDivergingPortion(
            final List<ScheduledLocation> remainder,
            final LocalDateTime lastRerouteTime,
            final ImmutableList.Builder<ScheduledLocation> scheduleBuilder) {

        final int returningIndex = getLocationIndex(
                remainder, returnStop);
        if (returningIndex != -1) {

            final ScheduledLocation returningScheduledLocation
                    = remainder.get(returningIndex);

            final TransitStop returningLocation
                    = returningScheduledLocation
                            .getLocation();
            final LocalDateTime originalReturnTime
                    = returningScheduledLocation
                            .getScheduledTime();
            LocalDateTime referenceTime = lastRerouteTime.plus(returnDelta);

            final ScheduledLocation newReturnScheduledLocation
                    = new ScheduledLocation(returningLocation, referenceTime);

            scheduleBuilder.add(newReturnScheduledLocation);

            LocalDateTime priorTime = originalReturnTime;

            for (int i = returningIndex + 1;
                 i < remainder.size(); i++) {
                final ScheduledLocation scheduledLocation = remainder.get(i);

                final TransitStop location = scheduledLocation.getLocation();
                final LocalDateTime originalTime 
                        = scheduledLocation.getScheduledTime();

                final Duration delta = Duration.between(
                        priorTime, originalTime);
                final LocalDateTime newTime = referenceTime.plus(delta);
                final ScheduledLocation newScheduledLocation
                        = new ScheduledLocation(location, newTime);
                referenceTime = newTime;
                scheduleBuilder.add(newScheduledLocation);
            }
        }
    }

    private List<ScheduledLocation> makeBackwardPostDivergingPortion(
            final List<ScheduledLocation> remainder,
            final LocalDateTime lastRerouteTime) {

        final ImmutableList.Builder<ScheduledLocation> portionBuilder
                = ImmutableList.builder();

        final int returningIndex = getLastLocationIndex(
                remainder, returnStop);
        if (returningIndex != -1) {

            final ScheduledLocation returningScheduledLocation
                    = remainder.get(returningIndex);

            final TransitStop returningLocation
                    = returningScheduledLocation.getLocation();
            final LocalDateTime originalReturnTime
                    = returningScheduledLocation.getScheduledTime();
            LocalDateTime referenceTime = lastRerouteTime.minus(returnDelta);

            final ScheduledLocation newReturnScheduledLocation
                    = new ScheduledLocation(returningLocation, referenceTime);

            portionBuilder.add(newReturnScheduledLocation);

            LocalDateTime priorTime = originalReturnTime;

            for (int i = returningIndex - 1; i >= 0; i--) {
                final ScheduledLocation scheduledLocation= remainder.get(i);

                final TransitStop location = scheduledLocation.getLocation();
                final LocalDateTime originalTime
                        = scheduledLocation.getScheduledTime();

                final Duration delta = Duration.between(
                        originalTime, priorTime);
                final LocalDateTime newTime = referenceTime.minus(delta);
                final ScheduledLocation newScheduledLocation
                        = new ScheduledLocation(location, newTime);
                referenceTime = newTime;
                portionBuilder.add(newScheduledLocation);
            }
        }
        return portionBuilder.build().reverse();
    }

    private static LocalDateTime makeForwardPreDivergingPortion(
            final List<ScheduledLocation> schedule, final int divergingIndex,
            final ImmutableList.Builder<ScheduledLocation> scheduleBuilder) {
        final List<ScheduledLocation> beginning
                = schedule.subList(0, divergingIndex + 1);

        scheduleBuilder.addAll(beginning);

        final ScheduledLocation lastLocation
                = beginning.get(beginning.size() - 1);
        final LocalDateTime lastTime = lastLocation.getScheduledTime();
        return lastTime;
    }

    private LocalDateTime makeForwardReroute(
            final LocalDateTime priorTime,
            final ImmutableList.Builder<ScheduledLocation> scheduleBuilder) {
        LocalDateTime referenceTime = priorTime;
        for (final RouteSequenceItem item : sequence) {
            final LocalDateTime time = referenceTime.plus(item.getDelta());
            final ScheduledLocation scheduledLocation
                    = new ScheduledLocation(item.getStop(), time);
            scheduleBuilder.add(scheduledLocation);
            referenceTime = time;
        }
        return referenceTime;
    }

    private List<ScheduledLocation> makeBackwardReroute(
            final LocalDateTime priorTime) {
        final ImmutableList.Builder<ScheduledLocation> rerouteBuilder
                = ImmutableList.builder();
        LocalDateTime referenceTime = priorTime;
        for (final RouteSequenceItem item : sequence) {
            final LocalDateTime time = referenceTime.minus(item.getDelta());
            final ScheduledLocation scheduledLocation
                    = new ScheduledLocation(item.getStop(), time);
            rerouteBuilder.add(scheduledLocation);
            referenceTime = time;
        }
        return rerouteBuilder.build().reverse();
    }

    private static int getLocationIndex(final List<ScheduledLocation> schedule,
                                        final TransitStop location) {
        int index = -1;
        for (int i = 0; i < schedule.size(); i++) {
            final ScheduledLocation scheduledLocation
                    = schedule.get(i);
            if (scheduledLocation.getLocation().equals(location)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private static int getLastLocationIndex(
            final List<ScheduledLocation> schedule,
            final TransitStop location) {
        int index = -1;
        for (int i = schedule.size() - 1; i >= 0; i--) {
            final ScheduledLocation scheduledLocation = schedule.get(i);
            if (scheduledLocation.getLocation().equals(location)) {
                index = i;
                break;
            }
        }
        return index;
    }

}
