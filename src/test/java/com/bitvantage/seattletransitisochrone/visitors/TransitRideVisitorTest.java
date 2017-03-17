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
package com.bitvantage.seattletransitisochrone.visitors;

import com.publictransitanalytics.scoregenerator.visitors.TransitRideVisitor;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.bitvantage.seattletransitisochrone.testhelpers.CountingVisitorFactory;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedLocalSchedule;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedRider;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedRiderFactory;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedTrip;
import com.publictransitanalytics.scoregenerator.tracking.ForwardMovingPath;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.tracking.TransitRideMovement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
public class TransitRideVisitorTest {

    private static final MovementPath PATH
            = new ForwardMovingPath(ImmutableList.of());
    private static final LocalDateTime KEY_TIME = LocalDateTime.of(
            2017, Month.JANUARY, 31, 10, 40, 0);
    private static final LocalDateTime DUMMY_TIME
            = LocalDateTime.of(2017, Month.FEBRUARY, 13, 8, 20, 0);

    @Test
    public void testAddsPathToSector() {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME,
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                PATH, null, 0, 5, ImmutableMap.of(),
                new PreloadedRiderFactory(new PreloadedRider(
                        Collections.emptySet(), null, null)),
                Collections.emptySet());

        visitor.visit(sector);

        Assert.assertEquals(Collections.singleton(PATH), sector.getPaths().get(
                            KEY_TIME));
    }

    @Test
    public void testAddsPathToContainingSector() throws Exception {
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
        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME,
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                PATH, null, 0, 5, ImmutableMap.of(),
                new PreloadedRiderFactory(new PreloadedRider(
                        Collections.emptySet(), null, null)),
                Collections.emptySet());

        visitor.visit(stop);

        Assert.assertEquals(Collections.singleton(PATH),
                            sector.getPaths().get(KEY_TIME));
    }

    @Test
    public void testAddsPathToTransitStop() throws Exception {
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
        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME,
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                PATH, null, 0, 5, ImmutableMap.of(),
                new PreloadedRiderFactory(new PreloadedRider(
                        Collections.emptySet(), null, null)),
                Collections.emptySet());

        visitor.visit(stop);

        Assert.assertEquals(Collections.singleton(PATH),
                            stop.getPaths().get(KEY_TIME));
    }

    @Test
    public void testDoesNotVisitLandmark() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME,
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                PATH, null, 0, 5, ImmutableMap.of(),
                new PreloadedRiderFactory(new PreloadedRider(
                        Collections.emptySet(), null, null)),
                Collections.emptySet());

        visitor.visit(landmark);
        Assert.assertTrue(landmark.getPaths().isEmpty());
    }

    @Test
    public void testVisitsStopOnTrip() throws Exception {
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

        final Movement movement = new TransitRideMovement(
                "tripId", "-1", "Somewhere via Elsewhere", "stop1", "Stop 1",
                DUMMY_TIME, "stop2", "Stop 2", DUMMY_TIME);

        final Trip trip = new PreloadedTrip(
                new TripId("tripId"), "Somewhere via Elsewhere", "-1",
                Collections.emptyList());
        final TransitStop nextStop = new TransitStop(
                sector, "2", "Elsehere", new Geodetic2DPoint(
                        new Longitude(-122.3087554, Longitude.DEGREES),
                        new Latitude(47.6755263, Latitude.DEGREES)));

        final PreloadedRider preloadedRider = new PreloadedRider(
                Collections.singleton(new RiderStatus(stop, DUMMY_TIME, trip)),
                Collections.singletonList(
                        new RiderStatus(nextStop, DUMMY_TIME, trip)), movement);

        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final LocalDateTime cutoffTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 50);
        final LocalDateTime currentTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 45);

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, cutoffTime, currentTime, PATH, null, 0, 5,
                ImmutableMap.of(stop, new PreloadedLocalSchedule(
                                Collections.emptyMap())),
                new PreloadedRiderFactory(preloadedRider),
                Collections.singleton(visitorFactory));

        visitor.visit(stop);

        Assert.assertEquals(1, visitorFactory.getVisitors().get(0)
                            .getTransitStopCount());
    }

    @Test
    public void testDoesNotVisitIfNoDepth() throws Exception {
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
        final Movement movement = new TransitRideMovement(
                "tripId", "-1", "Somewhere via Elsewhere", "stop1", "Stop 1",
                DUMMY_TIME, "stop2", "Stop 2", DUMMY_TIME);

        final Trip trip = new PreloadedTrip(
                new TripId("tripId"), "Somewhere via Elsewhere", "-1",
                Collections.emptyList());
        final TransitStop nextStop = new TransitStop(
                sector, "2", "Elsehere", new Geodetic2DPoint(
                        new Longitude(-122.3087554, Longitude.DEGREES),
                        new Latitude(47.6755263, Latitude.DEGREES)));

        final PreloadedRider preloadedRider = new PreloadedRider(
                Collections.singleton(new RiderStatus(stop, DUMMY_TIME, trip)),
                Collections.singletonList(new RiderStatus(
                        nextStop, DUMMY_TIME, trip)), movement);
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final LocalDateTime cutoffTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 50);
        final LocalDateTime currentTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 45);

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, cutoffTime, currentTime, PATH, null, 5, 5,
                ImmutableMap.of(stop, new PreloadedLocalSchedule(
                                Collections.emptyMap())),
                new PreloadedRiderFactory(preloadedRider),
                Collections.singleton(visitorFactory));

        visitor.visit(stop);

        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

    @Test
    public void testDoesNotVisitIfCannotContinue() throws Exception {
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
        final Movement movement = new TransitRideMovement(
                "tripId", "-1", "Somewhere via Elsewhere", "stop1", "Stop 1",
                DUMMY_TIME, "stop2", "Stop 2", DUMMY_TIME);

        final Trip trip = new PreloadedTrip(
                new TripId("tripId"), "Somewhere via Elsewhere", "-1",
                Collections.emptyList());
        final PreloadedRider rider = new PreloadedRider(
                Collections.singleton(new RiderStatus(stop, DUMMY_TIME, trip)),
                Collections.emptyList(), movement);

        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final LocalDateTime cutoffTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 50);
        final LocalDateTime currentTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 45);

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, cutoffTime, currentTime, PATH, null, 0, 5,
                ImmutableMap.of(stop, new PreloadedLocalSchedule(
                                Collections.emptyMap())),
                new PreloadedRiderFactory(rider),
                Collections.singleton(visitorFactory));

        visitor.visit(stop);

        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

    @Test
    public void testDoesNotVisitIfNoTrips() throws Exception {
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
        final Movement movement = new TransitRideMovement(
                "tripId", "-1", "Somewhere via Elsewhere", "stop1", "Stop 1",
                DUMMY_TIME, "stop2", "Stop 2", DUMMY_TIME);

        final PreloadedRider preloadedRider = new PreloadedRider(
                Collections.emptySet(), Collections.emptyList(), movement);
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final LocalDateTime cutoffTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 50);
        final LocalDateTime currentTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 45);

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, cutoffTime, currentTime, PATH, null, 0, 5,
                ImmutableMap.of(stop, new PreloadedLocalSchedule(
                                Collections.emptyMap())),
                new PreloadedRiderFactory(preloadedRider),
                Collections.singleton(visitorFactory));

        visitor.visit(stop);

        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

    @Test
    public void testDoesNotVisitIfBetterPath() throws Exception {
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
        final Movement movement = new TransitRideMovement(
                "tripId", "-1", "Somewhere via Elsewhere", "stop1", "Stop 1",
                DUMMY_TIME, "stop2", "Stop 2", DUMMY_TIME);

        final Trip trip = new PreloadedTrip(
                new TripId("tripId"), "Somewhere via Elsewhere", "-1",
                Collections.emptyList());
        final TransitStop nextStop = new TransitStop(
                sector, "2", "Elsewhere", new Geodetic2DPoint(
                        new Longitude(-122.3087554, Longitude.DEGREES),
                        new Latitude(47.6755263, Latitude.DEGREES)));
        nextStop.addPath(KEY_TIME, PATH);

        final PreloadedRider preloadedRider = new PreloadedRider(
                Collections.singleton(new RiderStatus(stop, DUMMY_TIME, trip)),
                Collections.singletonList(
                        new RiderStatus(nextStop, DUMMY_TIME, trip)), movement);
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final LocalDateTime cutoffTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 50);
        final LocalDateTime currentTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 45);

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, cutoffTime, currentTime, PATH, null, 0, 5,
                ImmutableMap.of(stop, new PreloadedLocalSchedule(
                                Collections.emptyMap())),
                new PreloadedRiderFactory(preloadedRider),
                Collections.singleton(visitorFactory));

        visitor.visit(stop);

        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

}
