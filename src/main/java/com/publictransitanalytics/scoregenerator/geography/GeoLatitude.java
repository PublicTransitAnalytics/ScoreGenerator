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
import org.opensextant.geodesy.Latitude;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
@EqualsAndHashCode
public class GeoLatitude implements Comparable<GeoLatitude>, GeoAngle {

    private final Latitude latitude;

    public GeoLatitude(final String measurement, final AngleUnit unit) {
        this(Double.valueOf(measurement), unit);
    }

    public GeoLatitude(final double measurement, final AngleUnit unit) {
        switch (unit) {
        case DEGREES:
            latitude = new Latitude(measurement, Latitude.DEGREES);
            break;
        case RADIANS:
            latitude = new Latitude(measurement, Latitude.RADIANS);
            break;

        default:
            throw new ScoreGeneratorFatalException(String.format(
                    "Unknown unit %s.", unit));

        }
    }

    public double getRadians() {
        return latitude.inRadians();
    }

    public double getDegrees() {
        return latitude.inDegrees();
    }

    public boolean inInterval(final GeoLatitude southLat,
                              final GeoLatitude northLat) {
        return latitude.inInterval(southLat.latitude, northLat.latitude);
    }

    public GeoPoint getIntersection(
            final Segment segment, final GeoBounds bounds) {

        final GeoPoint northPoint = segment.getNorthPoint();
        final GeoLongitude northLongitude = northPoint.getLongitude();
        final GeoLatitude northLatitude = northPoint.getLatitude();

        final GeoPoint southPoint = segment.getSouthPoint();
        final GeoLongitude southLongitude = southPoint.getLongitude();
        final GeoLatitude southLatitude = southPoint.getLatitude();

        if (latitude.equals(southLatitude.latitude)) {
            log.info("Exact southern match");
            return new GeoPoint(southLongitude, this);
        } else if (latitude.equals(northLatitude.latitude)) {
            log.info("Exact northern match");
            return new GeoPoint(northLongitude, this);
        } else if (inInterval(southLatitude, northLatitude)) {
            final GeoLongitude longitude1 = southLongitude;
            final GeoLatitude latitude1 = southLatitude;

            final GeoLongitude longitude2 = northLongitude;
            final GeoLatitude latitude2 = northLatitude;

            final double lat2Radians = latitude2.latitude.inRadians();
            final double lat1Radians = latitude1.latitude.inRadians();
            final double gridlineRadians = latitude.inRadians();
            final double lonDeltaRadians = longitude1.getRadians() -
                                           longitude2.getRadians();

            // Adapted from http://www.edwilliams.org/avform.htm
            final double sinLat2 = Math.sin(lat2Radians);
            final double cosLat2 = Math.cos(lat2Radians);
            final double cosLat1 = Math.cos(lat1Radians);
            final double sinLat1 = Math.sin(lat1Radians);
            final double cosGridline = Math.cos(gridlineRadians);
            final double sinGridline = Math.sin(gridlineRadians);
            final double sinLonDelta = Math.sin(lonDeltaRadians);
            final double cosLonDelta = Math.cos(lonDeltaRadians);

            final double product = sinLat1 * cosLat2 * cosGridline;

            final double a = product * sinLonDelta;
            final double b = (product * cosLonDelta) -
                             (cosLat1 * sinLat2 * cosGridline);
            final double c = cosLat1 * cosLat2 * sinGridline * sinLonDelta;
            final double lon = Math.atan2(b, a);

            final double aSquared = Math.pow(a, 2);
            final double bSquared = Math.pow(b, 2);
            final double sumOfSquares = aSquared + bSquared;
            final double root = Math.sqrt(sumOfSquares);

            if (Math.abs(c) > root) {
                log.info("No crossing on segment {}", segment);
                return null;
            } else {
                final double dlon = Math.acos(c / root);
                final double lon1Radians = longitude1.getRadians();

                final double crossRadians1 = GeoFormulae.geoMod(
                        lon1Radians + dlon + lon + Math.PI, 2 * Math.PI) -
                                             Math.PI;
                final GeoLongitude crossLongitude1
                        = new GeoLongitude(crossRadians1, AngleUnit.RADIANS);
                final double crossRadians2 = GeoFormulae.geoMod(
                        lon1Radians - dlon + lon + Math.PI, 2 * Math.PI) -
                                             Math.PI;
                final GeoLongitude crossLongitude2
                        = new GeoLongitude(crossRadians2, AngleUnit.RADIANS);

                if (crossLongitude1.inInterval(bounds.getWestLon(),
                                               bounds.getEastLon())) {
                    return new GeoPoint(crossLongitude1, this);
                }
                if (crossLongitude2.inInterval(bounds.getWestLon(),
                                               bounds.getEastLon())) {
                    return new GeoPoint(crossLongitude2, this);
                }
                log.info("No point recorded for {}: {} or {}",
                         segment, crossLongitude1, crossLongitude2);
            }
        }
        return null;
    }

    @Override
    public int compareTo(final GeoLatitude other) {
        return latitude.compareTo(other.latitude);
    }
    
    @Override
    public String toString() {
        return String.format("lat:%f", latitude.inRadians());
    }
}
