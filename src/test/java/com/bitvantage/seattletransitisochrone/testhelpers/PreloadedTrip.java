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
package com.bitvantage.seattletransitisochrone.testhelpers;

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedTrip implements Trip {

    @Getter
    private final TripId tripId;
    @Getter
    private final String routeName;
    @Getter
    private final String routeNumber;
    private final List<ScheduledLocation> stops;

    @Override
    public ScheduledLocation getNextScheduledLocation(
            final TransitStop stop, final LocalDateTime time) {
        int index = stops.stream().map(
                scheduledLocation -> scheduledLocation.getLocation()).collect(
                        Collectors.toList()).indexOf(stop);
        if (index == -1 || index == stops.size() - 1) {
            return null;
        }
        return stops.get(index + 1);
    }

    @Override
    public ScheduledLocation getPreviousScheduledLocation(
            final TransitStop stop, final LocalDateTime time) {
        int index = stops.stream().map(
                scheduledLocation -> scheduledLocation.getLocation()).collect(
                        Collectors.toList()).indexOf(stop);
        if (index == -1 || index == 0) {
            return null;
        }
        return stops.get(index - 1);
    }

}
