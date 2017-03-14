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
package com.bitvantage.seattletransitisochrone.rider;

import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRider;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.LocalSchedule;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedLocalSchedule;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedTrip;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.TransitRideMovement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
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
public class RetrospectiveRiderTest {

    private static final Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));
    private static final Geodetic2DPoint STOP_POINT = new Geodetic2DPoint(
            new Longitude(-122.325386, Longitude.DEGREES),
            new Latitude(47.63411, Latitude.DEGREES));

    private static final String STOP_ID = "-1";
    private static final String STOP_NAME = "Somewhere";
    private static final LocalDateTime STOP_TIME
            = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 30, 0);
    private static final TransitStop STOP_ON_TRIP = new TransitStop(
            SECTOR, STOP_ID, STOP_NAME, STOP_POINT);

    private static final LocalDateTime ANOTHER_STOP_TIME
            = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 5, 0);
    private static final String ANOTHER_STOP_ID = "-2";
    private static final String ANOTHER_STOP_NAME = "Elsewhere";
    private static final TransitStop ANOTHER_STOP_ON_TRIP = new TransitStop(
            SECTOR, ANOTHER_STOP_ID, ANOTHER_STOP_NAME, STOP_POINT);

    private static final String TRIP_BASE_ID = "trip1";

    private static final String ROUTE_NUMBER = "-1";
    private static final String ROUTE_NAME = "Somewhere via Elsewhere";

    @Test
    public void testFindsScheduled() {

        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, new PreloadedTrip(
                        new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                        ROUTE_NUMBER, ImmutableList.of(new ScheduledLocation(
                                STOP_ON_TRIP, STOP_TIME)))));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0));
        final Collection<Trip> trips = rider.getTrips(
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 45, 0));
        Assert.assertEquals(1, trips.size());
    }

    @Test
    public void testDoesNotFindOutsideOfWindow() {

        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, new PreloadedTrip(
                        new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                        ROUTE_NUMBER, ImmutableList.of(new ScheduledLocation(
                                STOP_ON_TRIP, STOP_TIME)))));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 35, 0));
        final Collection<Trip> trips = rider.getTrips(
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 45, 0));
        Assert.assertTrue(trips.isEmpty());
    }

    @Test
    public void testCannotContinueBeyondEnd() {
        final Trip trip = new PreloadedTrip(
                new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                ROUTE_NUMBER, ImmutableList.of(new ScheduledLocation(
                        STOP_ON_TRIP, STOP_TIME)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0));
        rider.takeTrip(trip);
        Assert.assertTrue(!rider.canContinueTrip());
    }

    @Test
    public void testCannotContinuePastCutoff() {
        final Trip trip = new PreloadedTrip(
                new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                ROUTE_NUMBER, ImmutableList.of(
                        new ScheduledLocation(
                                ANOTHER_STOP_ON_TRIP, ANOTHER_STOP_TIME),
                        new ScheduledLocation(STOP_ON_TRIP, STOP_TIME)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0));
        rider.takeTrip(trip);
        Assert.assertTrue(!rider.canContinueTrip());
    }

    @Test
    public void testCanContinue() {
        final Trip trip = new PreloadedTrip(
                new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                ROUTE_NUMBER, ImmutableList.of(
                        new ScheduledLocation(
                                ANOTHER_STOP_ON_TRIP, ANOTHER_STOP_TIME),
                        new ScheduledLocation(STOP_ON_TRIP, STOP_TIME)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0));
        rider.takeTrip(trip);
        Assert.assertTrue(rider.canContinueTrip());
    }

    @Test
    public void testCanContinueAtBoundary() {
        final Trip trip = new PreloadedTrip(
                new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                ROUTE_NUMBER, ImmutableList.of(
                        new ScheduledLocation(
                                ANOTHER_STOP_ON_TRIP, ANOTHER_STOP_TIME),
                        new ScheduledLocation(STOP_ON_TRIP, STOP_TIME)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 5, 0));
        rider.takeTrip(trip);
        Assert.assertTrue(rider.canContinueTrip());
    }

    @Test
    public void testContinues() {
        final Trip trip = new PreloadedTrip(
                new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                ROUTE_NUMBER, ImmutableList.of(
                        new ScheduledLocation(
                                ANOTHER_STOP_ON_TRIP, ANOTHER_STOP_TIME),
                        new ScheduledLocation(STOP_ON_TRIP, STOP_TIME)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0));
        rider.takeTrip(trip);

        final RiderStatus status = rider.continueTrip();
        Assert.assertEquals(ANOTHER_STOP_TIME, status.getTime());
        Assert.assertEquals(ANOTHER_STOP_ON_TRIP, status.getStop());

    }

    @Test
    public void testConstructsRideRecord() {
        final Trip trip = new PreloadedTrip(
                new TripId(TRIP_BASE_ID), "Somewhere via Elsewhere",
                ROUTE_NUMBER,
                ImmutableList.of(
                        new ScheduledLocation(
                                ANOTHER_STOP_ON_TRIP, ANOTHER_STOP_TIME),
                        new ScheduledLocation(STOP_ON_TRIP, STOP_TIME)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalSchedule schedule = new PreloadedLocalSchedule(path);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, schedule,
                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0));
        rider.takeTrip(trip);

        rider.continueTrip();
        final Movement movement = rider.getRideRecord();
        Assert.assertEquals(new TransitRideMovement(
                TRIP_BASE_ID, ROUTE_NUMBER, ROUTE_NAME, ANOTHER_STOP_ID, 
                ANOTHER_STOP_NAME, ANOTHER_STOP_TIME, STOP_ID, STOP_NAME, 
                STOP_TIME), movement);
    }

}
