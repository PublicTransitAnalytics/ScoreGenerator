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
package com.publictransitanalytics.scoregenerator.tracking;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.LocalDateTime;

/**
 *
 * @author Public Transit Analytics
 */
public class RetrospectivePath extends MovementPath {

    public RetrospectivePath() {
        this(ImmutableList.of());
    }

    public RetrospectivePath(final ImmutableList<Movement> movements) {
        super(movements);
    }

    @Override
    public MovementPath appendWalk(
            final PointLocation currentLocation,
            final LocalDateTime timeAtCurrentLocation,
            final VisitableLocation newLocation, 
            final LocalDateTime timeAtNewLocation, final WalkingCosts costs) {
        final WalkMovement movement = new WalkMovement(
                timeAtNewLocation, costs.getDistanceMeters(),
                newLocation.getNearestPoint(currentLocation.getLocation()),
                timeAtCurrentLocation, currentLocation.getLocation());
        return makeAppended(movement);
    }

    @Override
    public MovementPath appendTransitRide(
            final String tripId, final String routeNumber,
            final String routeName, final TransitStop currentStop,
            final LocalDateTime timeAtCurrentStop, final TransitStop newStop,
            final LocalDateTime timeAtNewStop) {
        final TransitRideMovement movement = new TransitRideMovement(
                tripId, routeNumber, routeName, newStop.getStopId(),
                newStop.getStopName(), timeAtNewStop, currentStop.getStopId(),
                currentStop.getStopName(), timeAtCurrentStop);

        return makeAppended(movement);
    }

    private MovementPath makeAppended(Movement movement) {
        return new RetrospectivePath(ImmutableList.<Movement>builder()
                .add(movement).addAll(movements).build());
    }

}
