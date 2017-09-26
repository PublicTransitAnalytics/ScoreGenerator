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

import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.LocalDateTime;
import lombok.Value;

/**
 * Tracks a move made by a transit ride.
 *
 * @author Public Transit Analytics
 */
@Value
public class TransitRideMovement implements Movement {

    private final Trip trip;
    private final VisitableLocation boardStop;
    private final LocalDateTime boardTime;
    private final VisitableLocation deboardStop;
    private final LocalDateTime deboardTime;

    @Override
    public LocalDateTime getStartTime() {
        return boardTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return deboardTime;
    }

    @Override
    public double getWalkingDistance() {
        return 0.0;
    }

    @Override
    public String getShortForm()
            throws InterruptedException {
        return trip.getRouteNumber();
    }

    @Override
    public String getMediumString()
            throws InterruptedException {
        return String.format("%s (%s)", trip.getRouteNumber(),
                             trip.getTripId());
    }

    @Override
    public String getLongForm()
            throws InterruptedException {
        return String.format("Take route %s from %s at %s arriving at %s at %s",
                trip.getRouteNumber(), boardStop.getCommonName(), boardTime,
                deboardStop.getCommonName(), deboardTime);
    }

}
