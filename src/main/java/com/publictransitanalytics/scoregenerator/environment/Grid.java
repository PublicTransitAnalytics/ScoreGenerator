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
package com.publictransitanalytics.scoregenerator.environment;

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Angle;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class Grid {

    final Set<PointLocation> points;

    public Grid(final Set<Segment> segments, final Geodetic2DBounds bounds,
                final int latitudeGridlines, final int longitudeGridlines) {

        final List<Segment> southSortedSegments = new ArrayList<>(segments);

        southSortedSegments.sort((segment, other) -> segment.getSouthPoint()
                .getLatitude().compareTo(other.getSouthPoint().getLatitude()));

        final Latitude southLat = bounds.getSouthLat();
        final Angle latitudeDifference = southLat.difference(
                bounds.getNorthLat());
        final double latitudeDeltaDegrees = latitudeDifference.inDegrees() /
                                            (latitudeGridlines + 1);
        final Angle latitudeDelta
                = new Angle(latitudeDeltaDegrees, Latitude.DEGREES);
        final Latitude firstLatitudeGridline
                = new Latitude(southLat.add(latitudeDelta));
        Latitude latitudeGridline = firstLatitudeGridline;

        final ImmutableSet.Builder<PointLocation> builder
                = ImmutableSet.builder();

        for (int i = 0; i < latitudeGridlines; i++) {
            for (final Segment segment : southSortedSegments) {

                final Geodetic2DPoint intersection = getLatitudeIntersection(
                        segment, latitudeGridline, bounds);
                if (intersection != null) {
                    builder.add(new Landmark(null, intersection));
                }
            }
            latitudeGridline = new Latitude(
                    latitudeGridline.add(latitudeDelta));
        }

        final List<Segment> westSortedSegments = new ArrayList<>(segments);
        westSortedSegments.sort((segment, other) -> segment.getWestPoint()
                .getLongitude().compareTo(other.getWestPoint().getLongitude()));

        final Longitude westLon = bounds.getWestLon();
        final Angle longitudeDifference = westLon.difference(
                bounds.getEastLon());
        final double longitudeDeltaDegrees = longitudeDifference.inDegrees() /
                                             (longitudeGridlines + 1);
        final Angle longitudeDelta
                = new Angle(longitudeDeltaDegrees, Longitude.DEGREES);
        final Longitude firstLongitudeGridline
                = new Longitude(westLon.add(longitudeDelta));
        Longitude longitudeGridline = firstLongitudeGridline;

        for (int i = 0; i < longitudeGridlines; i++) {
            for (final Segment segment : westSortedSegments) {
                final Geodetic2DPoint intersection = getLongitudeIntersection(
                        segment, longitudeGridline, bounds);
                if (intersection != null) {
                    builder.add(new Landmark(null, intersection));
                }
            }
            longitudeGridline = new Longitude(
                    longitudeGridline.add(longitudeDelta));
        }

        points = builder.build();
    }

    public Set<PointLocation> getGridPoints() {
        return points;
    }

    private Geodetic2DPoint getLongitudeIntersection(
            final Segment segment, final Longitude longitudeGridline,
            final Geodetic2DBounds bounds) {

        final Geodetic2DPoint eastPoint = segment.getEastPoint();
        final Longitude eastLongitude = eastPoint.getLongitude();
        final Latitude eastLatitude = eastPoint.getLatitude();

        final Geodetic2DPoint westPoint = segment.getWestPoint();
        final Longitude westLongitude = westPoint.getLongitude();
        final Latitude westLatitude = westPoint.getLatitude();

        if (westLongitude.compareTo(longitudeGridline) == 0) {
            log.info("Exact western match");
            return new Geodetic2DPoint(longitudeGridline, westLatitude);
        } else if (eastLongitude.compareTo(longitudeGridline) == 0) {
            log.info("Exact eastern match");
            return new Geodetic2DPoint(longitudeGridline, eastLatitude);
        } else if (longitudeGridline.inInterval(westLongitude, eastLongitude)) {
            // Adapted from http://www.edwilliams.org/avform.htm
            final Longitude longitude1 = eastLongitude;
            final Latitude latitude1 = eastLatitude;
            final double lon1Radians = longitude1.inRadians();
            final double lat1Radians = latitude1.inRadians();

            final Longitude longitude2 = westLongitude;
            final Latitude latitude2 = westLatitude;
            final double lon2Radians = longitude2.inRadians();
            final double lat2Radians = latitude2.inRadians();

            final double lonGridlineRadians = longitudeGridline.inRadians();

            final double cosLat2 = Math.cos(lat2Radians);
            final double cosLat1 = Math.cos(lat1Radians);
            final double latRadians = Math.atan(
                    ((Math.sin(lat1Radians) * cosLat2 *
                      Math.sin(lonGridlineRadians - lon2Radians)) -
                     (Math.sin(lat2Radians) * cosLat1 *
                      Math.sin(lonGridlineRadians - lon1Radians))) /
                    (cosLat1 * cosLat2 * Math.sin(lon1Radians - lon2Radians)));
            final Latitude latitude = new Latitude(latRadians,
                                                   Latitude.RADIANS);

            if (latitude.inInterval(bounds.getSouthLat(),
                                    bounds.getNorthLat())) {
                return new Geodetic2DPoint(longitudeGridline, latitude);
            }
        }
        return null;
    }

    private Geodetic2DPoint getLatitudeIntersection(
            final Segment segment, final Latitude latitudeGridline,
            final Geodetic2DBounds bounds) {

        final Geodetic2DPoint northPoint = segment.getNorthPoint();
        final Longitude northLongitude = northPoint.getLongitude();
        final Latitude northLatitude = northPoint.getLatitude();

        final Geodetic2DPoint southPoint = segment.getSouthPoint();
        final Longitude southLongitude = southPoint.getLongitude();
        final Latitude southLatitude = southPoint.getLatitude();

        if (southLatitude.compareTo(latitudeGridline) == 0) {
            log.info("Exact southern match");
            return new Geodetic2DPoint(southLongitude, latitudeGridline);
        } else if (northLatitude.compareTo(latitudeGridline) == 0) {
            log.info("Exact northern match");
            return new Geodetic2DPoint(northLongitude, latitudeGridline);
        } else if (latitudeGridline.inInterval(southLatitude,
                                               northLatitude)) {
            final Longitude longitude1 = southLongitude;
            final Latitude latitude1 = southLatitude;

            final Longitude longitude2 = northLongitude;
            final Latitude latitude2 = northLatitude;

            final double lat2Radians = latitude2.inRadians();
            final double lat1Radians = latitude1.inRadians();
            final double gridlineRadians = latitudeGridline.inRadians();
            final double lonDeltaRadians = longitude1.inRadians() -
                                           longitude2.inRadians();

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
            final double c = cosLat1 * cosLat2 * sinGridline *
                             sinLonDelta;
            final double lon = Math.atan2(b, a);

            final double sumOfSquares = Math.pow(a, 2) + Math.pow(b, 2);
            final double root = Math.sqrt(sumOfSquares);

            if (Math.abs(c) > root) {
                log.info("No crossing on segment {}", segment);
                return null;
            } else {
                final double dlon = Math.acos(c / root);
                final double lon1Radians = longitude1.inRadians();

                final double crossRadians1
                        = geoMod(lon1Radians + dlon + lon + Math.PI,
                                 2 * Math.PI) - Math.PI;
                final Longitude crossLongitude1 = new Longitude(
                        crossRadians1, Longitude.RADIANS);
                final double crossRadians2
                        = geoMod(lon1Radians - dlon + lon + Math.PI,
                                 2 * Math.PI) - Math.PI;
                final Longitude crossLongitude2 = new Longitude(
                        crossRadians2, Longitude.RADIANS);

                if (crossLongitude1.inInterval(bounds.getWestLon(),
                                               bounds.getEastLon())) {
                    return new Geodetic2DPoint(crossLongitude1,
                                               latitudeGridline);
                }
                if (crossLongitude2.inInterval(bounds.getWestLon(),
                                               bounds.getEastLon())) {
                    return new Geodetic2DPoint(crossLongitude2,
                                               latitudeGridline);
                }
                log.info("No point recorded for {}: {} or {}",
                         segment, crossLongitude1, crossLongitude2);
            }
        }
        return null;
    }

    private static double geoMod(final double y, final double x) {
        double mod = y - x * (int) (y / x);
        return (mod < 0) ? mod + x : mod;
    }

}
