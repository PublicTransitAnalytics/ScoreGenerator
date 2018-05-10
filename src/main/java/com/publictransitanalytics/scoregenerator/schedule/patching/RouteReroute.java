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
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
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
        final List<VehicleEvent> schedule = original.getSchedule();
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
                        final List<VehicleEvent> newSchedule
                                = Appending.appendToSchedule(schedule,
                                                             sequence);
                        newTrip = Appending.makeReplacementTrip(original,
                                                                newSchedule);
                    } else if (type.equals(ReferenceDirection.BEFORE_FIRST)) {
                        final List<VehicleEvent> newSchedule
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
                final ImmutableList.Builder<VehicleEvent> scheduleBuilder
                        = ImmutableList.builder();
                if (type.equals(ReferenceDirection.AFTER_LAST)) {
                    final int divergingIndex = getFirstLocationIndex(
                            schedule, referenceStop);

                    if (divergingIndex == -1) {
                        newTrip = original;
                    } else {
                        final List<VehicleEvent> beginning
                                = schedule.subList(0, divergingIndex + 1);

                        scheduleBuilder.addAll(beginning);

                        final VehicleEvent lastLocation
                                = beginning.get(beginning.size() - 1);
                        final LocalDateTime divergingTime = lastLocation
                                .getDepartureTime();

                        final List<VehicleEvent> rereoute
                                = makeForwardReroute(divergingTime);
                        scheduleBuilder.addAll(rereoute);
                        final LocalDateTime lastRerouteTimeDeparture
                                = rereoute.get(rereoute.size() - 1)
                                        .getDepartureTime();

                        if (returnStop != null) {
                            final List<VehicleEvent> end
                                    = makeForwardPostDivergingPortion(
                                            schedule, lastRerouteTimeDeparture);
                            scheduleBuilder.addAll(end);
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
                        final List<VehicleEvent> end = schedule.subList(
                                divergingIndex, schedule.size());
                        final LocalDateTime divergingTime
                                = end.get(0).getArrivalTime();

                        final List<VehicleEvent> reroute
                                = makeBackwardReroute(divergingTime);
                        final LocalDateTime lastRerouteArrivalTime
                                = reroute.get(0).getArrivalTime();

                        if (returnStop != null) {
                            final List<VehicleEvent> beginning
                                    = makeBackwardPostDivergingPortion(
                                            schedule, lastRerouteArrivalTime);
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

    private List<VehicleEvent> makeForwardPostDivergingPortion(
            final List<VehicleEvent> schedule,
            final LocalDateTime lastRerouteDepartureTime) {

        final ImmutableList.Builder<VehicleEvent> scheduleBuilder
                = ImmutableList.builder();
        final int returningIndex = getFirstLocationIndex(schedule, returnStop);

        if (returningIndex != -1) {
            final List<VehicleEvent> remainder
                    = schedule.subList(returningIndex, schedule.size());
            
            final VehicleEvent returningEvent = remainder.get(0);

            final LocalDateTime originalBase = returningEvent.getArrivalTime();
            final LocalDateTime newBase
                    = lastRerouteDepartureTime.plus(returnDelta);

            for (final VehicleEvent event : remainder) {
                final VehicleEvent newEvent = getAdjustedEvent(
                        event, originalBase, newBase);
                scheduleBuilder.add(newEvent);
            }
        }
        return scheduleBuilder.build();
    }

    private List<VehicleEvent> makeBackwardPostDivergingPortion(
            final List<VehicleEvent> schedule,
            final LocalDateTime lastRerouteArrivalTime) {

        final ImmutableList.Builder<VehicleEvent> scheduleBuilder
                = ImmutableList.builder();

        final int returningIndex = getLastLocationIndex(schedule, returnStop);

        if (returningIndex != -1) {
            final List<VehicleEvent> remainder
                    = schedule.subList(0, returningIndex + 1);

            final VehicleEvent returningEvent 
                    = remainder.get(remainder.size() - 1);

            final LocalDateTime originalBase
                    = returningEvent.getDepartureTime();
            final LocalDateTime newBase
                    = lastRerouteArrivalTime.minus(returnDelta);

            for (final VehicleEvent event : remainder) {
                final VehicleEvent newEvent = getAdjustedEvent(
                        event, originalBase, newBase);
                scheduleBuilder.add(newEvent);
            }
        }
        return scheduleBuilder.build();
    }

    private VehicleEvent getAdjustedEvent(final VehicleEvent event,
                                          final LocalDateTime originalBase,
                                          final LocalDateTime newBase) {
        final TransitStop location = event.getLocation();
        final LocalDateTime originalArrivalTime
                = event.getArrivalTime();
        final LocalDateTime originalDepartureTime
                = event.getDepartureTime();

        final Duration arrivalDelta = Duration.between(
                originalBase, originalArrivalTime);
        final Duration departureDelta = Duration.between(
                originalBase, originalDepartureTime);
        final LocalDateTime newArrivalTime = newBase.plus(arrivalDelta);
        final LocalDateTime newDepartureTime
                = newBase.plus(departureDelta);
        final VehicleEvent newEvent = new VehicleEvent(location, newArrivalTime,
                                                       newDepartureTime);
        return newEvent;
    }

    private List<VehicleEvent> makeForwardReroute(
            final LocalDateTime priorDepartureTime) {
        final ImmutableList.Builder<VehicleEvent> scheduleBuilder
                = ImmutableList.builder();
        LocalDateTime referenceDepartureTime = priorDepartureTime;
        for (final RouteSequenceItem item : sequence) {
            final LocalDateTime arrivalTime
                    = referenceDepartureTime.plus(item.getDelta());
            // TODO: Allow dwell in reroutes.
            final LocalDateTime departureTime = arrivalTime;
            final VehicleEvent scheduledLocation
                    = new VehicleEvent(item.getStop(), arrivalTime,
                                       departureTime);
            scheduleBuilder.add(scheduledLocation);
            referenceDepartureTime = departureTime;
        }
        return scheduleBuilder.build();
    }

    private List<VehicleEvent> makeBackwardReroute(
            final LocalDateTime priorArrivalTime) {
        final ImmutableList.Builder<VehicleEvent> rerouteBuilder
                = ImmutableList.builder();
        LocalDateTime referenceArrivalTime = priorArrivalTime;
        for (final RouteSequenceItem item : sequence) {
            final LocalDateTime departureTime
                    = referenceArrivalTime.minus(item.getDelta());
            // TODO: Allow dwell in reroutes.
            final LocalDateTime arrivalTime = departureTime;
            final VehicleEvent scheduledLocation
                    = new VehicleEvent(item.getStop(), arrivalTime,
                                       departureTime);
            rerouteBuilder.add(scheduledLocation);
            referenceArrivalTime = arrivalTime;
        }
        return rerouteBuilder.build().reverse();
    }

    private static int getFirstLocationIndex(final List<VehicleEvent> schedule,
                                             final TransitStop location) {
        int index = -1;
        for (int i = 0; i < schedule.size(); i++) {
            final VehicleEvent scheduledLocation
                    = schedule.get(i);
            if (scheduledLocation.getLocation().equals(location)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private static int getLastLocationIndex(
            final List<VehicleEvent> schedule,
            final TransitStop location) {
        int index = -1;
        for (int i = schedule.size() - 1; i >= 0; i--) {
            final VehicleEvent scheduledLocation = schedule.get(i);
            if (scheduledLocation.getLocation().equals(location)) {
                index = i;
                break;
            }
        }
        return index;
    }

}
