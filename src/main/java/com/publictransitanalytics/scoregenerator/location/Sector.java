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

import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import lombok.Getter;
import lombok.ToString;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 * A Sector is a portion of the grid overlayed on the service region.
 *
 * @author Public Transit Analytics
 */
@ToString
public class Sector extends VisitableLocation {

    @Getter
    private final Geodetic2DBounds bounds;
    
    private final String boundsString;
    
    public Sector(final Geodetic2DBounds bounds) {
        this.bounds = bounds;
        boundsString = bounds.toString();
    }
    
    public boolean contains(final Geodetic2DPoint location) {
        return bounds.contains(location);
    }

    @Override
    public String getIdentifier() {
        return boundsString;
    }

    @Override
    public String getCommonName() {
        return boundsString;
    }

    @Override
    public Geodetic2DPoint getNearestPoint(
            final Geodetic2DPoint givenLocation) {
        if (bounds.contains(givenLocation)) {
            return givenLocation;
        }

        final Latitude latitude = givenLocation.getLatitude();
        final Latitude targetLatitude;
        if (latitude.compareTo(bounds.getNorthLat()) > 0) {
            targetLatitude = bounds.getNorthLat();
        } else if (latitude.compareTo(bounds.getSouthLat()) < 0) {
            targetLatitude = bounds.getSouthLat();
        } else {
            targetLatitude = latitude;
        }

        final Longitude longitude = givenLocation.getLongitude();
        final Longitude targetLongitude;
        if (longitude.compareTo(bounds.getEastLon()) > 0) {
            targetLongitude = bounds.getEastLon();
        } else if (longitude.compareTo(bounds.getWestLon()) < 0) {
            targetLongitude = bounds.getWestLon();
        } else {
            targetLongitude = longitude;
        }

        return new Geodetic2DPoint(targetLongitude, targetLatitude);
    }

    @Override
    public Geodetic2DPoint getCanonicalPoint() {
        return bounds.getCenter();
    }

    @Override
    public void accept(Visitor visitor) throws InterruptedException {
        visitor.visit(this);
    }

}
