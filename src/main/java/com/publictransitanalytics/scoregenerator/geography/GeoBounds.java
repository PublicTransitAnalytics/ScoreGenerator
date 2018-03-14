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

import com.google.common.collect.ImmutableSortedSet;
import java.util.NavigableSet;
import lombok.EqualsAndHashCode;
import org.opensextant.geodesy.Geodetic2DArc;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
@EqualsAndHashCode(exclude = {"degreeString", "radianString"})
public class GeoBounds {

    private final Geodetic2DBounds bounds;
    private final String degreeString;
    private final String radianString;

    public GeoBounds(
            final GeoLongitude westLon, final GeoLatitude southLat,
            final GeoLongitude eastLon, final GeoLatitude northLat) {
        bounds = new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(westLon.getRadians(), Longitude.RADIANS),
                        new Latitude(southLat.getRadians(), Latitude.RADIANS)),
                new Geodetic2DPoint(
                        new Longitude(eastLon.getRadians(), Longitude.RADIANS),
                        new Latitude(northLat.getRadians(), Latitude.RADIANS)));
        degreeString = String.format(
                "%f, %f, %f, %f", westLon.getDegrees(), southLat.getDegrees(),
                eastLon.getDegrees(), northLat.getDegrees());
        radianString = String.format(
                "%f, %f, %f, %f", westLon.getRadians(), southLat.getRadians(),
                eastLon.getRadians(), northLat.getRadians());

    }

    public boolean contains(final GeoPoint point) {
        return bounds.contains(new Geodetic2DPoint(
                new Longitude(point.getLongitude().getRadians(),
                              Longitude.RADIANS),
                new Latitude(point.getLatitude().getRadians(),
                             Latitude.RADIANS)));
    }

    public GeoPoint getCenter() {
        final Geodetic2DPoint center = bounds.getCenter();
        return new GeoPoint(new GeoLongitude(center.getLongitude().inRadians(),
                                             AngleUnit.RADIANS),
                            new GeoLatitude(center.getLatitude().inRadians(),
                                            AngleUnit.RADIANS));
    }

    public double getLongitudinalDistance() {
        final Latitude midpoint = bounds.getCenter().getLatitude();
        final Geodetic2DPoint eastPoint = new Geodetic2DPoint(
                bounds.getEastLon(), midpoint);
        final Geodetic2DPoint westPoint = new Geodetic2DPoint(
                bounds.getWestLon(), midpoint);
        final Geodetic2DArc arc = new Geodetic2DArc(eastPoint, westPoint);
        return arc.getDistanceInMeters();
    }

    public double getLatitudinalDistance() {
        final Longitude midpoint = bounds.getCenter().getLongitude();
        final Geodetic2DPoint eastPoint = new Geodetic2DPoint(
                midpoint, bounds.getNorthLat());
        final Geodetic2DPoint westPoint = new Geodetic2DPoint(
                midpoint, bounds.getSouthLat());
        final Geodetic2DArc arc = new Geodetic2DArc(eastPoint, westPoint);
        return arc.getDistanceInMeters();
    }

    public NavigableSet<GeoLatitude> getLatitudeGridlines(final int amount) {
        final Latitude southLat = bounds.getSouthLat();
        final double southLatRadians = southLat.inRadians();
        final Latitude northLat = bounds.getNorthLat();
        final double northLatRadians = northLat.inRadians();
        final double delta = (northLatRadians - southLatRadians) / (amount - 1);

        final ImmutableSortedSet.Builder<GeoLatitude> builder
                = ImmutableSortedSet.naturalOrder();
        double radians = southLatRadians;
        for (int i = 0; i < amount; i++) {
            builder.add(new GeoLatitude(radians, AngleUnit.RADIANS));
            radians += delta;
        }
        return builder.build();
    }

    public NavigableSet<GeoLongitude> getLongitudeGridlines(final int amount) {
        final Longitude westLon = bounds.getWestLon();
        final double westLonRadians = westLon.inRadians();
        final Longitude eastLon = bounds.getEastLon();
        final double eastLonRadians = eastLon.inRadians();
        final double delta = (eastLonRadians - westLonRadians) / (amount - 1);

        final ImmutableSortedSet.Builder<GeoLongitude> builder
                = ImmutableSortedSet.naturalOrder();
        double radians = westLonRadians;
        for (int i = 0; i < amount; i++) {
            builder.add(new GeoLongitude(radians, AngleUnit.RADIANS));
            radians += delta;
        }
        return builder.build();
    }

    public GeoLatitude getSouthLat() {
        return new GeoLatitude(bounds.getSouthLat().inRadians(),
                               AngleUnit.RADIANS);
    }

    public GeoLatitude getNorthLat() {
        return new GeoLatitude(bounds.getNorthLat().inRadians(),
                               AngleUnit.RADIANS);
    }

    public GeoLongitude getWestLon() {
        return new GeoLongitude(bounds.getWestLon().inRadians(),
                                AngleUnit.RADIANS);
    }

    public GeoLongitude getEastLon() {
        return new GeoLongitude(bounds.getEastLon().inRadians(),
                                AngleUnit.RADIANS);
    }

    public String toDegreeString() {
        return degreeString;
    }

    @Override
    public String toString() {
        return radianString;
    }

    public static GeoBounds parseRadianString(final String boundsString) {
        final String boundStrings[] = boundsString.split(",");
        final GeoLongitude westLon = new GeoLongitude(boundStrings[0],
                                                      AngleUnit.RADIANS);
        final GeoLatitude southLat = new GeoLatitude(boundStrings[1],
                                                     AngleUnit.RADIANS);
        final GeoLongitude eastLon = new GeoLongitude(boundStrings[2],
                                                      AngleUnit.RADIANS);
        final GeoLatitude northLat = new GeoLatitude(boundStrings[3],
                                                     AngleUnit.RADIANS);
        return new GeoBounds(westLon, southLat, eastLon, northLat);
    }

}
