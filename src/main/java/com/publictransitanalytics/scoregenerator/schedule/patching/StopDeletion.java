/*
 * Copyright 2018 Public Transit Analytics.
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
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class StopDeletion implements Patch {

    final TransitStop deletedStop;

    @Override
    public Optional<Trip> patch(final Trip original) {
        final List<VehicleEvent> newSchedule = original.getSchedule().stream()
                .filter(event -> !event.getLocation()
                .equals(deletedStop)).collect(Collectors.toList());

        if (newSchedule.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new Trip(
                    original.getTripId(), original.getRouteName(),
                    original.getRouteNumber(), newSchedule));
        }
    }

}
