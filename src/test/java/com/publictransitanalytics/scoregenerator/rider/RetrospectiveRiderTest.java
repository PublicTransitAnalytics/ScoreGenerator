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
package com.publictransitanalytics.scoregenerator.rider;

import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.ScheduleEntry;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
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

    private static final LocalDateTime EARLIER_STOP_TIME
            = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 5, 0);
    private static final String EARLIER_STOP_ID = "-2";
    private static final String EARLIER_STOP_NAME = "Elsewhere";
    private static final TransitStop EARLIER_STOP_ON_TRIP = new TransitStop(
            SECTOR, EARLIER_STOP_ID, EARLIER_STOP_NAME, STOP_POINT);

    private static final String TRIP_BASE_ID = "trip1";
    private static final LocalDate TRIP_SERVICE_DAY
            = LocalDate.of(2017, Month.FEBRUARY, 12);

    private static final String ROUTE_NUMBER = "-1";
    private static final String ROUTE_NAME = "Somewhere via Elsewhere";

    @Test
    public void testCannotContinueBeyondEnd() {
        final Trip trip = new Trip(
                new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), ROUTE_NAME,
                ROUTE_NUMBER, ImmutableSet.of(
                        new ScheduleEntry(0, STOP_TIME, STOP_ON_TRIP)));
        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
                STOP_TIME, trip));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, STOP_TIME, cutoffTime, trip);
        Assert.assertTrue(!rider.canContinueTrip());
    }

    @Test
    public void testCannotContinuePastCutoff() {
        final Trip trip = new Trip(
                new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), ROUTE_NAME,
                ROUTE_NUMBER, ImmutableSet.of(
                        new ScheduleEntry(0, EARLIER_STOP_TIME,
                                          EARLIER_STOP_ON_TRIP),
                        new ScheduleEntry(1, STOP_TIME, STOP_ON_TRIP)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, STOP_TIME, cutoffTime, trip);
        Assert.assertTrue(!rider.canContinueTrip());
    }

    @Test
    public void testCanContinue() {
        final Trip trip = new Trip(
                new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), ROUTE_NAME,
                ROUTE_NUMBER, ImmutableSet.of(
                        new ScheduleEntry(0, EARLIER_STOP_TIME,
                                          EARLIER_STOP_ON_TRIP),
                        new ScheduleEntry(1, STOP_TIME, STOP_ON_TRIP)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, STOP_TIME, cutoffTime, trip);
        Assert.assertTrue(rider.canContinueTrip());
    }

    @Test
    public void testCanContinueAtBoundary() {
        final Trip trip = new Trip(
                new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), ROUTE_NAME,
                ROUTE_NUMBER, ImmutableSet.of(
                        new ScheduleEntry(0, EARLIER_STOP_TIME,
                                          EARLIER_STOP_ON_TRIP),
                        new ScheduleEntry(1, STOP_TIME, STOP_ON_TRIP)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 5, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, STOP_TIME, cutoffTime, trip);
        Assert.assertTrue(rider.canContinueTrip());
    }

    @Test
    public void testContinues() {
        final Trip trip = new Trip(
                new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), ROUTE_NAME,
                ROUTE_NUMBER, ImmutableSet.of(
                        new ScheduleEntry(0, EARLIER_STOP_TIME,
                                          EARLIER_STOP_ON_TRIP),
                        new ScheduleEntry(1, STOP_TIME, STOP_ON_TRIP)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                STOP_ON_TRIP, STOP_TIME, cutoffTime, trip);

        final RiderStatus status = rider.continueTrip();
        Assert.assertEquals(EARLIER_STOP_TIME, status.getTime());
        Assert.assertEquals(EARLIER_STOP_ON_TRIP, status.getStop());

    }

}
