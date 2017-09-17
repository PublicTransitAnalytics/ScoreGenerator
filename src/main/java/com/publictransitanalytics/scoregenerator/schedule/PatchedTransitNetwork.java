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

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Public Transit Analytics
 */
public class PatchedTransitNetwork implements TransitNetwork {

    private final Set<Trip> allowedTrips;
    private final TransitNetwork baseNetwork;

    public PatchedTransitNetwork(final Set<String> removedRoutes,
                                 final TransitNetwork baseNetwork) {
        this.baseNetwork = baseNetwork;
        allowedTrips = baseNetwork.getTrips().stream().filter(
                trip -> !removedRoutes.contains(trip.getRouteNumber()))
                .collect(Collectors.toSet());

    }

    @Override
    public Set<EntryPoint> getEntryPoints(final TransitStop stop,
                                          final LocalDateTime startTime,
                                          final LocalDateTime endTime) {
        final Set<EntryPoint> unfilteredEntryPoints
                = baseNetwork.getEntryPoints(stop, startTime, endTime);
        return unfilteredEntryPoints.stream().filter(
                entryPoint -> allowedTrips.contains(entryPoint.getTrip()))
                .collect(Collectors.toSet());
    }

    @Override
    public Duration getInServiceTime() {
        Duration duration = Duration.ZERO;
        for (final Trip trip : allowedTrips) {
            duration = duration.plus(trip.getInServiceTime());
        }
        return duration;
    }

    @Override
    public Set<Trip> getTrips() {
        return allowedTrips;
    }

}
