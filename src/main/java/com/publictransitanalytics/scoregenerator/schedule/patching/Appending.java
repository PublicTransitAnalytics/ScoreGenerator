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
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author Public Transit Analytics
 */
public class Appending {

    public static Trip makeReplacementTrip(
            final Trip originalTrip,
            final List<ScheduledLocation> newStops) {
        return new Trip(originalTrip.getTripId(), originalTrip.getRouteName(),
                        originalTrip.getRouteNumber(), newStops);
    }

    public static List<ScheduledLocation> appendToSchedule(
            final List<ScheduledLocation> schedule,
            final List<RouteSequenceItem> extension) {
        final LocalDateTime baseTime
                = schedule.get(schedule.size() - 1).getScheduledTime();
        final ImmutableList.Builder<ScheduledLocation> builder
                = ImmutableList.builder();
        builder.addAll(schedule);
        LocalDateTime lastTime = baseTime;
        for (final RouteSequenceItem item : extension) {
            final LocalDateTime newTime = lastTime.plus(item.getDelta());
            builder.add(new ScheduledLocation(item.getStop(), newTime));
            lastTime = newTime;
        }
        return builder.build();
    }

    public static List<ScheduledLocation> prependToSchedule(
            final List<ScheduledLocation> schedule,
            final List<RouteSequenceItem> extension) {
        final LocalDateTime baseTime = schedule.get(0).getScheduledTime();
        final ImmutableList.Builder<ScheduledLocation> extensionBuilder
                = ImmutableList.builder();
        LocalDateTime lastTime = baseTime;
        for (final RouteSequenceItem item : extension) {
            final LocalDateTime newTime = lastTime.minus(item.getDelta());
            extensionBuilder.add(new ScheduledLocation(item.getStop(), newTime));
            lastTime = newTime;
        }
        return ImmutableList.<ScheduledLocation>builder().addAll(
                extensionBuilder.build().reverse()).addAll(schedule).build();
    }

}
