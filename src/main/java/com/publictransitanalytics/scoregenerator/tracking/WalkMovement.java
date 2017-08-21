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
import java.time.LocalDateTime;
import lombok.Value;

/**
 * Tracks a move made by walking.
 *
 * @author Public Transit Analytics
 */
@Value
public class WalkMovement implements Movement {

    private final LocalDateTime startTime;
    private final double walkingDistance;
    private final VisitableLocation startLocation;
    private final LocalDateTime endTime;
    private final VisitableLocation endLocation;

    public WalkMovement(
            final LocalDateTime startTime, final double walkingDistance,
            final VisitableLocation startLocation, final LocalDateTime endTime,
            final VisitableLocation endLocation) {
        this.startTime = startTime;
        this.walkingDistance = walkingDistance;
        this.startLocation = startLocation;
        this.endTime = endTime;
        this.endLocation = endLocation;
    }

    @Override
    public String getShortForm()
            throws InterruptedException {
        return "Walk";
    }

    @Override
    public String getMediumString() {
        return "Walk";
    }
    
     @Override
    public String getLongForm()
            throws InterruptedException {
       
        return String.format(
                "Walk from %s at %s arriving at %s at %s",
                startLocation.toString(), startTime, endLocation.toString(),
                endTime);
    }

}
