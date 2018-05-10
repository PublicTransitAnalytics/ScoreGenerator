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

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class RetrospectiveRiderTest {

    private static final GeoPoint STOP_POINT = new GeoPoint(
            new GeoLongitude("-122.32539", AngleUnit.DEGREES),
            new GeoLatitude("47.63411", AngleUnit.DEGREES));

    private static final String STOP_ID = "-1";
    private static final String STOP_NAME = "Somewhere";
    private static final LocalDateTime STOP_TIME
            = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 30, 0);
    private static final TransitStop STOP_ON_TRIP = new TransitStop(
            STOP_ID, STOP_NAME, STOP_POINT);

    private static final LocalDateTime EARLIER_STOP_TIME
            = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 5, 0);
    private static final String EARLIER_STOP_ID = "-2";
    private static final String EARLIER_STOP_NAME = "Elsewhere";
    private static final TransitStop EARLIER_STOP_ON_TRIP = new TransitStop(
            EARLIER_STOP_ID, EARLIER_STOP_NAME, STOP_POINT);

    private static final String TRIP_BASE_ID = "trip1";
    private static final LocalDate TRIP_SERVICE_DAY
            = LocalDate.of(2017, Month.FEBRUARY, 12);
    private static final TripId TRIP_ID
            = new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY);

    private static final String ROUTE_NUMBER = "-1";
    private static final String ROUTE_NAME = "Somewhere via Elsewhere";

    @Test
    public void testCannotContinueBeyondEnd() {
        final Trip trip = new Trip(
                TRIP_ID, ROUTE_NAME, ROUTE_NUMBER, ImmutableList.of(
                        new VehicleEvent(STOP_ON_TRIP, STOP_TIME, STOP_TIME)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                new EntryPoint(trip, STOP_TIME, 0), cutoffTime);
        Assert.assertTrue(!rider.canContinueTrip());
    }

    @Test
    public void testCannotContinuePastCutoff() {
        final Trip trip = new Trip(
                TRIP_ID, ROUTE_NAME, ROUTE_NUMBER, ImmutableList.of(
                        new VehicleEvent(EARLIER_STOP_ON_TRIP,
                                         EARLIER_STOP_TIME, EARLIER_STOP_TIME),
                        new VehicleEvent(STOP_ON_TRIP, STOP_TIME, STOP_TIME)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                new EntryPoint(trip, STOP_TIME, 1), cutoffTime);
        Assert.assertTrue(!rider.canContinueTrip());
    }

    @Test
    public void testCanContinue() {
        final Trip trip = new Trip(
                TRIP_ID, ROUTE_NAME, ROUTE_NUMBER, ImmutableList.of(
                        new VehicleEvent(EARLIER_STOP_ON_TRIP,
                                         EARLIER_STOP_TIME, EARLIER_STOP_TIME),
                        new VehicleEvent(STOP_ON_TRIP, STOP_TIME, STOP_TIME)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                new EntryPoint(trip, STOP_TIME, 1), cutoffTime);
        Assert.assertTrue(rider.canContinueTrip());
    }

    @Test
    public void testCanContinueAtBoundary() {
        final Trip trip = new Trip(
                TRIP_ID, ROUTE_NAME, ROUTE_NUMBER, ImmutableList.of(
                        new VehicleEvent(EARLIER_STOP_ON_TRIP,
                                         EARLIER_STOP_TIME, EARLIER_STOP_TIME),
                        new VehicleEvent(STOP_ON_TRIP, STOP_TIME, STOP_TIME)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 5, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                new EntryPoint(trip, STOP_TIME, 1), cutoffTime);
        Assert.assertTrue(rider.canContinueTrip());
    }

    @Test
    public void testContinues() {
        final Trip trip = new Trip(
                TRIP_ID, ROUTE_NAME, ROUTE_NUMBER, ImmutableList.of(
                        new VehicleEvent(EARLIER_STOP_ON_TRIP,
                                         EARLIER_STOP_TIME, EARLIER_STOP_TIME),
                        new VehicleEvent(STOP_ON_TRIP, STOP_TIME, STOP_TIME)));

        final LocalDateTime cutoffTime
                = LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 0, 0);

        final RetrospectiveRider rider = new RetrospectiveRider(
                new EntryPoint(trip, STOP_TIME, 1), cutoffTime);

        final RiderStatus status = rider.continueTrip();
        Assert.assertEquals(EARLIER_STOP_TIME, status.getTime());
        Assert.assertEquals(EARLIER_STOP_ON_TRIP, status.getStop());

    }

}
