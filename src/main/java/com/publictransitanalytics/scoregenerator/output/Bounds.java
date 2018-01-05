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
package com.publictransitanalytics.scoregenerator.output;

import com.publictransitanalytics.scoregenerator.environment.SectorTable;
import com.publictransitanalytics.scoregenerator.location.Sector;
import org.opensextant.geodesy.Geodetic2DBounds;

/**
 * A description of bounds that produces a comma-separated list of bounds.
 * 
 * @author Public Transit Analytics
 */
public class Bounds {

    private final String boundsString;

    public Bounds(final Sector sector) {
        this(sector.getBounds());
    }

    public Bounds(final SectorTable sectorTable) {
        this(sectorTable.getBounds());
    }

    public Bounds(final Geodetic2DBounds bounds) {
        boundsString = String.format(
                "%f,%f,%f,%f", bounds.getNorthLat().inDegrees(),
                bounds.getSouthLat().inDegrees(),
                bounds.getEastLon().inDegrees(),
                bounds.getWestLon().inDegrees());
    }

    @Override
    public String toString() {
        return boundsString;
    }

}
