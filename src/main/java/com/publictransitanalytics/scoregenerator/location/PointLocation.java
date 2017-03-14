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
package com.publictransitanalytics.scoregenerator.location;

import lombok.Getter;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * A location that exists at a single coordinate.
 *
 * @author Public Transit Analytics
 */
public abstract class PointLocation extends VisitableLocation {

    @Getter
    private final Sector containingSector;
    @Getter
    private final Geodetic2DPoint location;

    public PointLocation(final Sector containingSector,
                         final Geodetic2DPoint location) {
        this.containingSector = containingSector;
        this.location = location;
    }

    @Override
    public Geodetic2DPoint getNearestPoint(final Geodetic2DPoint givenLocation) {
        return location;
    }

    @Override
    public Geodetic2DPoint getCanonicalPoint() {
        return location;
    }

}
