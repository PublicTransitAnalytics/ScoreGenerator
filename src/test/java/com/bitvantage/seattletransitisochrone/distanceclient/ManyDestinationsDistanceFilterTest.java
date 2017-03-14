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
package com.bitvantage.seattletransitisochrone.distanceclient;

import com.publictransitanalytics.scoregenerator.distanceclient.ManyDestinationsDistanceFilter;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedDistanceClient;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableTable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
public class ManyDestinationsDistanceFilterTest {

    @Test
    public void testLeavesOutSame() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));

        final ManyDestinationsDistanceFilter filter
                = new ManyDestinationsDistanceFilter(
                        new PreloadedDistanceClient(ImmutableTable.of()));
        final Map<VisitableLocation, WalkingCosts> distances
                = filter.getFilteredDistances(
                        stop, Collections.singleton(stop),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0));
        Assert.assertTrue(distances.isEmpty());
    }

    @Test
    public void testLeavesOutContaining() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));

        final ManyDestinationsDistanceFilter filter
                = new ManyDestinationsDistanceFilter(
                        new PreloadedDistanceClient(ImmutableTable.of()));
        final Map<VisitableLocation, WalkingCosts> distances
                = filter.getFilteredDistances(
                        stop, Collections.singleton(sector),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0));
        Assert.assertTrue(distances.isEmpty());
    }

    @Test
    public void testReachesInRange() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TransitStop otherStop = new TransitStop(
                sector, "2", "Elsewhere", new Geodetic2DPoint(
                        new Longitude(-122.3406012, Longitude.DEGREES),
                        new Latitude(47.6246984, Latitude.DEGREES)));

        final ManyDestinationsDistanceFilter filter
                = new ManyDestinationsDistanceFilter(
                        new PreloadedDistanceClient(ImmutableTable.of(
                                stop, otherStop, new WalkingCosts(
                                        Duration.ofMinutes(1), 1.1))));
        final Map<VisitableLocation, WalkingCosts> distances
                = filter.getFilteredDistances(
                        stop, Collections.singleton(otherStop),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 15, 0));
        Assert.assertEquals(1, distances.size());
    }

    @Test
    public void testFiltersOutOfRange() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TransitStop otherStop = new TransitStop(
                sector, "2", "Elsewhere", new Geodetic2DPoint(
                        new Longitude(-122.3406012, Longitude.DEGREES),
                        new Latitude(47.6246984, Latitude.DEGREES)));

        final ManyDestinationsDistanceFilter filter
                = new ManyDestinationsDistanceFilter(
                        new PreloadedDistanceClient(ImmutableTable.of(
                                stop, otherStop, new WalkingCosts(
                                        Duration.ofMinutes(2), 1.1))));
        final Map<VisitableLocation, WalkingCosts> distances
                = filter.getFilteredDistances(
                        stop, Collections.singleton(otherStop),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 11, 0));
        Assert.assertTrue(distances.isEmpty());
    }

    @Test
    public void testFiltersOutNoResults() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TransitStop otherStop = new TransitStop(
                sector, "2", "Elsewhere", new Geodetic2DPoint(
                        new Longitude(-122.3406012, Longitude.DEGREES),
                        new Latitude(47.6246984, Latitude.DEGREES)));

        final ManyDestinationsDistanceFilter filter
                = new ManyDestinationsDistanceFilter(
                        new PreloadedDistanceClient(ImmutableTable.of()));
        final Map<VisitableLocation, WalkingCosts> distances
                = filter.getFilteredDistances(
                        stop, Collections.singleton(otherStop),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 10, 0),
                        LocalDateTime.of(2017, Month.FEBRUARY, 13, 12, 11, 0));
        Assert.assertTrue(distances.isEmpty());
    }

}
