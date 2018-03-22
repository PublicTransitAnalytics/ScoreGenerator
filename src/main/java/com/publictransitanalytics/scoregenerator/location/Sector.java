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

import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import lombok.Getter;

/**
 * A Sector is a portion of the grid overlayed on the service region.
 *
 * @author Public Transit Analytics
 */
public class Sector implements LogicalCenter {

    @Getter
    private final GeoBounds bounds;

    private final String boundsString;
    private final String boundsDegreeString;

    public Sector(final GeoBounds bounds) {
        this.bounds = bounds;
        boundsString = bounds.toString();
        boundsDegreeString = bounds.toDegreeString();
    }

    public boolean contains(final GeoPoint location) {
        return bounds.contains(location);
    }

    public String getIdentifier() {
        return boundsString;
    }

    public String getCommonName() {
        return boundsDegreeString;
    }

    public GeoPoint getCenter() {
        return bounds.getCenter();
    }

    @Override
    public String toString() {
        return boundsString;
    }

    @Override
    public GeoPoint getPointRepresentation() {
        return getCenter();
    }

}
