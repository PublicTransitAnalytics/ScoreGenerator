/*
 * Copyright 2018 Public Transit Analytics.
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
package com.publictransitanalytics.scoregenerator;

import lombok.EqualsAndHashCode;
import org.opensextant.geodesy.Geodetic2DArc;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
@EqualsAndHashCode(exclude = {"degreeString", "radianString"})
public class GeoPoint {

    private final Geodetic2DPoint point;
    private final String radianString;
    private final String degreeString;

    public GeoPoint(final GeoLongitude longitude,
                    final GeoLatitude latitude) {
        point = new Geodetic2DPoint(
                new Longitude(longitude.getRadians(), Longitude.RADIANS),
                new Latitude(latitude.getRadians(), Latitude.RADIANS));
        radianString = String.format("%f, %f", latitude.getRadians(),
                                     longitude.getRadians());
        degreeString = String.format("%f, %f", latitude.getDegrees(),
                                     longitude.getDegrees());
    }

    public GeoLatitude getLatitude() {
        return new GeoLatitude(point.getLatitude().inRadians(),
                               AngleUnit.RADIANS);
    }

    public GeoLongitude getLongitude() {
        return new GeoLongitude(point.getLongitude().inRadians(),
                                AngleUnit.RADIANS);
    }

    public double getDistanceMeters(final GeoPoint other) {
        return new Geodetic2DArc(point, other.point).getDistanceInMeters();
    }

    public String toDegreeString() {
        return degreeString;
    }

    @Override
    public String toString() {
        return radianString;
    }

    public static GeoPoint parseDegreeString(final String location) {
        final String coordinateStrings[] = location.split(",");
        final GeoLatitude latitude = new GeoLatitude(coordinateStrings[0],
                                                     AngleUnit.DEGREES);
        final GeoLongitude longitude = new GeoLongitude(coordinateStrings[1],
                                                        AngleUnit.DEGREES);
        return new GeoPoint(longitude, latitude);
    }

}
