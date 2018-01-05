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
public class RouteTruncation implements Patch {

    private final String routeNumber;
    private final TransitStop referenceStop;
    private final ReferenceDirection type;

    @Override
    public Optional<Trip> patch(final Trip original) {
        final Trip newTrip;
        final List<ScheduledLocation> schedule = original.getSchedule();
        if (routeNumber.equals(original.getRouteNumber())) {
            if (schedule.isEmpty() || schedule.size() == 1) {
                newTrip = original;
                log.info("Trip {} was not truncated because it is too short.",
                         newTrip.getTripId());
            } else {
                if (type.equals(ReferenceDirection.AFTER_LAST)) {
                    int index = -1;
                    for (int i = 0; i < schedule.size(); i++) {
                        if (schedule.get(i).getLocation()
                                .equals(referenceStop)) {
                            index = i;
                            break;
                        }
                    }
                    newTrip = (index != -1) ? makeReplacementTrip(
                            original, schedule.subList(0, index + 1))
                            : original;
                } else if (type.equals(ReferenceDirection.BEFORE_FIRST)) {
                    int index = -1;
                    for (int i = schedule.size() - 1; i >= 0; i--) {
                        if (schedule.get(i).getLocation()
                                .equals(referenceStop)) {
                            index = i;
                            break;
                        }
                    }
                    newTrip = (index != -1) ? makeReplacementTrip(
                            original, schedule.subList(index, schedule.size()))
                            : original;
                } else {
                    newTrip = original;
                }
            }
        } else {
            newTrip = original;
        }

        return Optional.of(newTrip);
    }

    private static Trip makeReplacementTrip(
            final Trip originalTrip,
            final List<ScheduledLocation> newStops) {
        return new Trip(originalTrip.getTripId(), originalTrip.getRouteName(),
                        originalTrip.getRouteNumber(), newStops);
    }

}
