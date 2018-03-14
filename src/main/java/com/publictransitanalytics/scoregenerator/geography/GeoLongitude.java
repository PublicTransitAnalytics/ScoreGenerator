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
package com.publictransitanalytics.scoregenerator.geography;

import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.environment.Segment;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
@EqualsAndHashCode
public class GeoLongitude implements Comparable<GeoLongitude>, GeoAngle {

    private final Longitude longitude;

    public GeoLongitude(final String measurement,
                        final AngleUnit unit) {
        this(Double.valueOf(measurement), unit);
    }

    public GeoLongitude(final double measurement,
                        final AngleUnit unit) {
        switch (unit) {
        case DEGREES:
            longitude = new Longitude(measurement, Longitude.DEGREES);
            break;
        case RADIANS:
            longitude = new Longitude(measurement, Longitude.RADIANS);
            break;

        default:
            throw new ScoreGeneratorFatalException(String.format(
                    "Unknown unit %s.", unit));

        }
    }

    public double getRadians() {
        return longitude.inRadians();
    }

    public double getDegrees() {
        return longitude.inDegrees();
    }

    public boolean inInterval(final GeoLongitude westLon,
                              final GeoLongitude eastLon) {
        return longitude.inInterval(westLon.longitude, eastLon.longitude);
    }

    public GeoPoint getIntersection(
            final Segment segment, final GeoBounds bounds) {

        final GeoPoint eastPoint = segment.getEastPoint();
        final GeoLongitude eastLongitude = eastPoint.getLongitude();
        final GeoLatitude eastLatitude = eastPoint.getLatitude();

        final GeoPoint westPoint = segment.getWestPoint();
        final GeoLongitude westLongitude = westPoint.getLongitude();
        final GeoLatitude westLatitude = westPoint.getLatitude();

        if (longitude.equals(westLongitude.longitude)) {
            log.info("Exact western match");
            return new GeoPoint(this, westLatitude);
        } else if (longitude.equals(eastLongitude.longitude)) {
            log.info("Exact eastern match");
            return new GeoPoint(this, eastLatitude);
        } else if (inInterval(westLongitude, eastLongitude)) {
            // Adapted from http://www.edwilliams.org/avform.htm
            final GeoLongitude longitude1 = eastLongitude;
            final GeoLatitude latitude1 = eastLatitude;
            final double lon1Radians = longitude1.longitude.inRadians();
            final double lat1Radians = latitude1.getRadians();

            final GeoLongitude longitude2 = westLongitude;
            final GeoLatitude latitude2 = westLatitude;
            final double lon2Radians = longitude2.longitude.inRadians();
            final double lat2Radians = latitude2.getRadians();

            final double lonGridlineRadians = longitude.inRadians();

            final double sinLat1 = Math.sin(lat1Radians);
            final double cosLat2 = Math.cos(lat2Radians);
            final double sinDeltaLon2 = Math.sin(
                    lonGridlineRadians - lon2Radians);
            final double sinLat2 = Math.sin(lat2Radians);
            final double cosLat1 = Math.cos(lat1Radians);
            final double sinDeltaLon1 = Math.sin(
                    lonGridlineRadians - lon1Radians);
            final double sinDeltaLon12 = Math.sin(
                    lon1Radians - lon2Radians);

            final double firstTerm = sinLat1 * cosLat2 * sinDeltaLon2;
            final double subtrahend = sinLat2 * cosLat1 * sinDeltaLon1;
            final double divisor = cosLat1 * cosLat2 * sinDeltaLon12;

            final double latRadians = Math.atan(
                    (firstTerm - subtrahend) / divisor);
            final GeoLatitude latitude
                    = new GeoLatitude(latRadians, AngleUnit.RADIANS);

            if (latitude.inInterval(bounds.getSouthLat(),
                                    bounds.getNorthLat())) {
                return new GeoPoint(this, latitude);
            }
        }
        return null;
    }

    @Override
    public int compareTo(final GeoLongitude other) {
        return longitude.compareTo(other.longitude);
    }

    @Override
    public String toString() {
        return String.format("lon:%f", longitude.inRadians());
    }

}
