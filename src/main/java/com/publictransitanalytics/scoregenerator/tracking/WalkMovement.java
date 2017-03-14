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

import java.time.LocalDateTime;
import lombok.Value;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * Tracks a move made by walking.
 *
 * @author Public Transit Analytics
 */
@Value
public class WalkMovement implements Movement {

    private final LocalDateTime startTime;
    private final double walkingDistance;
    private final Geodetic2DPoint startLocation;
    private final LocalDateTime endTime;
    private final Geodetic2DPoint endLocation;

    @Override
    public String getShortForm() {
        return "Walk";
    }

    @Override
    public String getDestinationString() {
        return endLocation.toString();
    }

    @Override
    public String getOriginString() {
        return startLocation.toString();
    }

    @Override
    public String getMediumString() {
        return "Walk";
    }
}
