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
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public abstract class MovementPath implements Comparable<MovementPath> {

    @Getter
    protected final ImmutableList<Movement> movements;

    public Duration getDuration() {
        if (movements.isEmpty()) {
            return Duration.ZERO;
        }
        final LocalDateTime startTime = movements.get(0).getStartTime();
        final LocalDateTime endTime
                = movements.get(movements.size() - 1).getEndTime();
        return Duration.between(startTime, endTime);
    }

    public abstract MovementPath appendWalk(
            final PointLocation currentLocation,
            final LocalDateTime timeAtCurrentLocation,
            final VisitableLocation newLocation, 
            final LocalDateTime timeAtNewLocation, final WalkingCosts costs);

    public abstract MovementPath appendTransitRide(
            final String tripId, final String routeNumber,
            final String routeName, final TransitStop currentStop,
            final LocalDateTime timeAtCurrentStop,
            final TransitStop newStop, final LocalDateTime timeAtNewStop);

    private double getWalkingDistance() {
        double walkingDistance = 0;
        for (Movement modeChange : movements) {
            walkingDistance += modeChange.getWalkingDistance();
        }
        return walkingDistance;
    }

    @Override
    public int compareTo(final MovementPath o) {
        if (movements.isEmpty()) {
            // If we are empty, the other path is worse, unless it's empty too.
            return -o.movements.size();
        }
        if (o.movements.isEmpty()) {
            // If the other one is empty, we're worse, unless we're empty too.
            return movements.size();
        }

        final LocalDateTime endTime
                = movements.get(movements.size() - 1).getEndTime();
        final LocalDateTime otherEndTime
                = o.movements.get(o.movements.size() - 1).getEndTime();

        final LocalDateTime startTime
                = movements.get(0).getStartTime();
        final LocalDateTime otherStartTime
                = o.movements.get(0).getStartTime();

        // What gets me there first?
        if (!endTime.equals(otherEndTime)) {
            return endTime.compareTo(otherEndTime);
        }
        // What lets me start the latest?
        if (!startTime.equals(otherStartTime)) {
            return -startTime.compareTo(otherStartTime);
        }
        // What gets me there with the least walking?
        if (Double.compare(getWalkingDistance(), o.getWalkingDistance())
                    != 0) {
            return Double.compare(getWalkingDistance(), o
                                  .getWalkingDistance());
        }
        
        // What gets me there with the fewest changes in mode?
        if (movements.size()
                    != o.movements.size()) {
            return movements.size() - o.movements.size();
        }
        // Break ties arbitrarily.

        return System.identityHashCode(
                this) - System.identityHashCode(o);
    }

}
