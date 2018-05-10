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
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author Public Transit Analytics
 */
public final class Appending {

    public static Trip makeReplacementTrip(
            final Trip originalTrip,
            final List<VehicleEvent> newStops) {
        return new Trip(originalTrip.getTripId(), originalTrip.getRouteName(),
                        originalTrip.getRouteNumber(), newStops);
    }

    public static List<VehicleEvent> appendToSchedule(
            final List<VehicleEvent> schedule,
            final List<RouteSequenceItem> extension) {
        final LocalDateTime baseDepartureTime
                = schedule.get(schedule.size() - 1).getDepartureTime();
        final ImmutableList.Builder<VehicleEvent> builder
                = ImmutableList.builder();
        builder.addAll(schedule);
        LocalDateTime lastTime = baseDepartureTime;
        for (final RouteSequenceItem item : extension) {
            final LocalDateTime arrivalTime = lastTime.plus(item.getDelta());
            final LocalDateTime departureTime = arrivalTime;
            builder.add(new VehicleEvent(item.getStop(), arrivalTime,
                                         departureTime));
            lastTime = departureTime;
        }
        return builder.build();
    }

    public static List<VehicleEvent> prependToSchedule(
            final List<VehicleEvent> schedule,
            final List<RouteSequenceItem> extension) {
        final LocalDateTime baseArrivalTime = schedule.get(0).getArrivalTime();
        final ImmutableList.Builder<VehicleEvent> extensionBuilder
                = ImmutableList.builder();
        LocalDateTime lastTime = baseArrivalTime;
        for (final RouteSequenceItem item : extension) {
            final LocalDateTime arrivalTime = lastTime.minus(item.getDelta());
            final LocalDateTime departureTime = arrivalTime;
            extensionBuilder.add(new VehicleEvent(item.getStop(), arrivalTime,
                                                  departureTime));
            lastTime = departureTime;
        }
        return ImmutableList.<VehicleEvent>builder().addAll(
                extensionBuilder.build().reverse()).addAll(schedule).build();
    }
    
    private Appending() { }

}
