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

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
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
public class RouteExtension implements Patch {

    private final String routeNumber;
    private final TransitStop referenceStop;
    private final ReferenceDirection type;
    private final List<RouteSequenceItem> extensionSequence;

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
                                = Appending.appendToSchedule(
                                        schedule, extensionSequence);
                        newTrip = Appending.makeReplacementTrip(original,
                                                                newSchedule);
                    } else if (type.equals(ReferenceDirection.BEFORE_FIRST)) {
                        final List<ScheduledLocation> newSchedule
                                = Appending.prependToSchedule(
                                        schedule, extensionSequence);
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
                if (type.equals(ReferenceDirection.AFTER_LAST)) {
                    final TransitStop lastStop = schedule.get(
                            schedule.size() - 1).getLocation();
                    if (referenceStop.equals(lastStop)) {
                        final List<ScheduledLocation> newSchedule
                                = Appending.appendToSchedule(
                                        schedule, extensionSequence);
                        newTrip = Appending.makeReplacementTrip(original,
                                                                newSchedule);
                    } else {
                        newTrip = original;
                        log.info(
                                "Refenece stop {} not last in trip {} ({} was)",
                                referenceStop.getIdentifier(),
                                newTrip.getTripId(), lastStop.getIdentifier());
                    }
                } else if (type.equals(ReferenceDirection.BEFORE_FIRST)) {
                    final TransitStop firstStop
                            = schedule.get(0).getLocation();
                    if (referenceStop.equals(firstStop)) {
                        final List<ScheduledLocation> newSchedule
                                = Appending.prependToSchedule(
                                        schedule, extensionSequence);
                        newTrip = Appending.makeReplacementTrip(
                                original, newSchedule);
                    } else {
                        newTrip = original;
                        log.info(
                                "Reference stop {} not first in trip {} ({} was)",
                                referenceStop.getIdentifier(),
                                newTrip.getTripId(), firstStop.getIdentifier());
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
}
