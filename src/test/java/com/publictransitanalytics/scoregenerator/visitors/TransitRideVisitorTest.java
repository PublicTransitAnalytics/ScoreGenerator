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
package com.publictransitanalytics.scoregenerator.visitors;

import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.testhelpers.CountingVisitorFactory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedRider;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedRiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedScheduleReader;
import com.publictransitanalytics.scoregenerator.tracking.ForwardMovingPath;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import java.time.LocalDate;
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

    private static final LocalDate TRIP_SERVICE_DAY
            = LocalDate.of(2017, Month.JANUARY, 21);

    private final LocalDateTime CUTOFF_TIME = LocalDateTime.of(
            2017, Month.JANUARY, 21, 10, 50);
    private final LocalDateTime CURRENT_TIME = LocalDateTime.of(
            2017, Month.JANUARY, 21, 10, 45);

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
                KEY_TIME, CURRENT_TIME, CUTOFF_TIME, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(
                        new PreloadedScheduleReader(Collections.emptySet()),
                        new PreloadedRider(Collections.emptyList())),
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
                KEY_TIME, CURRENT_TIME, CUTOFF_TIME, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(
                        new PreloadedScheduleReader(Collections.emptySet()),
                        new PreloadedRider(Collections.emptyList())),
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
                KEY_TIME, CURRENT_TIME, CUTOFF_TIME, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(
                        new PreloadedScheduleReader(Collections.emptySet()),
                        new PreloadedRider(Collections.emptyList())),
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
                KEY_TIME, CURRENT_TIME, CUTOFF_TIME, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(
                        new PreloadedScheduleReader(Collections.emptySet()),
                        new PreloadedRider(Collections.emptyList())),
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

       final Trip trip = new Trip(
                new TripId("tripId", TRIP_SERVICE_DAY),
                "Somewhere via Elsewhere", "-1", Collections.emptySet());
        final TransitStop nextStop = new TransitStop(
                sector, "2", "Elsehere", new Geodetic2DPoint(
                        new Longitude(-122.3087554, Longitude.DEGREES),
                        new Latitude(47.6755263, Latitude.DEGREES)));

        final PreloadedScheduleReader reader = new PreloadedScheduleReader(
                Collections.singleton(new EntryPoint(trip, DUMMY_TIME)));
        final PreloadedRider rider = new PreloadedRider(
                Collections.singletonList(
                        new RiderStatus(nextStop, DUMMY_TIME, trip)));

        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, CUTOFF_TIME, CURRENT_TIME, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(reader, rider),
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

        final Trip trip = new Trip(new TripId(
                "tripId", TRIP_SERVICE_DAY),
                "Somewhere via Elsewhere", "-1", Collections.emptySet());
        final TransitStop nextStop = new TransitStop(
                sector, "2", "Elsehere", new Geodetic2DPoint(
                        new Longitude(-122.3087554, Longitude.DEGREES),
                        new Latitude(47.6755263, Latitude.DEGREES)));

        final PreloadedScheduleReader reader = new PreloadedScheduleReader(
                Collections.singleton(new EntryPoint(trip, DUMMY_TIME)));
        final PreloadedRider rider = new PreloadedRider(
                Collections.singletonList(
                        new RiderStatus(nextStop, DUMMY_TIME, trip)));
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, CUTOFF_TIME, CURRENT_TIME, PATH, null, 5, 5,
                new PreloadedRiderBehaviorFactory(reader, rider),
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

        final Trip trip = new Trip(
                new TripId("tripId", TRIP_SERVICE_DAY),
                "Somewhere via Elsewhere", "-1", Collections.emptySet());
        
        final PreloadedScheduleReader reader = new PreloadedScheduleReader(
                Collections.singleton(new EntryPoint(trip, DUMMY_TIME)));
        final PreloadedRider rider = new PreloadedRider(
                Collections.emptyList());
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, CUTOFF_TIME, CURRENT_TIME, PATH, null, 0, 5,   
                new PreloadedRiderBehaviorFactory(reader, rider),
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

        final PreloadedScheduleReader reader = new PreloadedScheduleReader(
                Collections.emptySet());
        final PreloadedRider rider = new PreloadedRider(
                Collections.emptyList());
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, CUTOFF_TIME, CURRENT_TIME, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(reader, rider),
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

        final Trip trip = new Trip(
                new TripId("tripId", TRIP_SERVICE_DAY),
                "Somewhere via Elsewhere", "-1", Collections.emptySet());
        final TransitStop nextStop = new TransitStop(
                sector, "2", "Elsewhere", new Geodetic2DPoint(
                        new Longitude(-122.3087554, Longitude.DEGREES),
                        new Latitude(47.6755263, Latitude.DEGREES)));
        nextStop.addPath(KEY_TIME, PATH);

        final PreloadedScheduleReader reader = new PreloadedScheduleReader(
                Collections.singleton(new EntryPoint(trip, DUMMY_TIME)));
        final PreloadedRider rider = new PreloadedRider(
                Collections.singletonList(
                        new RiderStatus(nextStop, DUMMY_TIME, trip)));
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final LocalDateTime cutoffTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 50);
        final LocalDateTime currentTime = LocalDateTime.of(
                2017, Month.JANUARY, 21, 10, 45);

        final TransitRideVisitor visitor = new TransitRideVisitor(
                KEY_TIME, cutoffTime, currentTime, PATH, null, 0, 5,
                new PreloadedRiderBehaviorFactory(reader, rider),
                Collections.singleton(visitorFactory));

        visitor.visit(stop);

        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

}
