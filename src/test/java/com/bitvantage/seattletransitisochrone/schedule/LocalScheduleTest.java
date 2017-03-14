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
package com.bitvantage.seattletransitisochrone.schedule;

import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingLocalSchedule;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedRouteDetailsDirectory;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedServiceTypeCalendar;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedStopTimesDirectory;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedTripDetailsDirectory;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
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
public class LocalScheduleTest {

    private final static Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));

    private final static String ASSOCIATED_STOP_ID = "stopId1";

    private final static TransitStop ASSOCIATED_STOP = new TransitStop(
            SECTOR, ASSOCIATED_STOP_ID, "name1", new Geodetic2DPoint(
                    new Longitude(-122.324966, Longitude.DEGREES),
                    new Latitude(47.6647377, Latitude.DEGREES)));

    private final String ROUTE_ID_1 = "routeId1";

    private final String TRIP_ID_1 = "tripId1";

    private final String WEEKDAY_SERVICE = "WEEKDAY";

    @Test
    public void testFindsTripInRange() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 9, (byte) 11, (byte) 0),
                ASSOCIATED_STOP_ID,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 26),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1,
                                        WEEKDAY_SERVICE))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertEquals(1, trips.size());
    }

    @Test
    public void testFindsTripAtRangeStart() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 9, (byte) 10, (byte) 0),
                ASSOCIATED_STOP_ID,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 26),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1,
                                        WEEKDAY_SERVICE))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertEquals(1, trips.size());
    }

    @Test
    public void testFindsTripAtRangeEnd() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 9, (byte) 20, (byte) 0),
                ASSOCIATED_STOP_ID,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 26),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1,
                                        WEEKDAY_SERVICE))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertEquals(1, trips.size());
    }

    @Test
    public void testFindsTripFromPreviousServiceDay() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 33, (byte) 11, (byte) 0),
                ASSOCIATED_STOP_ID,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 25),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1,
                                        WEEKDAY_SERVICE))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertEquals(1, trips.size());
    }

    @Test
    public void testDoesNotFindTripOutOfRange() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 9, (byte) 30, (byte) 0),
                ASSOCIATED_STOP_ID,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 26),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1,
                                        WEEKDAY_SERVICE))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertTrue(trips.isEmpty());
    }

    @Test
    public void testDoesNotFindTripNotRunningOnDay() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 9, (byte) 11, (byte) 0),
                ASSOCIATED_STOP_ID,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 26),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1, "WEEKEND"))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertTrue(trips.isEmpty());
    }

    @Test
    public void testDoesNotFindTripAtDifferentStop() {
        final TripStop stop = new TripStop(
                new TransitTime((byte) 9, (byte) 11, (byte) 0),
                "stopId2",
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);
        final DirectoryReadingLocalSchedule schedule = new DirectoryReadingLocalSchedule(
                ASSOCIATED_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 10, 9, 0),
                new PreloadedStopTimesDirectory(Collections.singleton(stop)),
                new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID_1, new RouteDetails("1", "Downtown Seattle"))),
                new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        LocalDate.of(2017, Month.JANUARY, 26),
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE)))),
                new PreloadedTripDetailsDirectory(ImmutableMap.of(
                        new TripGroupKey(TRIP_ID_1),
                        new TripDetails(TRIP_ID_1, ROUTE_ID_1,
                                        WEEKDAY_SERVICE))),
                ImmutableMap.of(ASSOCIATED_STOP_ID, ASSOCIATED_STOP));

        final Collection<Trip> trips = schedule.getTripsInRange(
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 10, 0),
                LocalDateTime.of(2017, Month.JANUARY, 26, 9, 20, 0));
        Assert.assertTrue(trips.isEmpty());
    }

}
