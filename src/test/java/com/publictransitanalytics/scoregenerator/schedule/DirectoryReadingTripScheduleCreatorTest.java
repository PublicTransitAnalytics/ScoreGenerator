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
package com.publictransitanalytics.scoregenerator.schedule;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedRouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedStopTimesDirectory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedTripDetailsDirectory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.TripDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStops;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.testhelpers.TimePair;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class DirectoryReadingTripScheduleCreatorTest {

    private static final GeoPoint POINT = new GeoPoint(
            new GeoLongitude("-122.33618", AngleUnit.DEGREES),
            new GeoLatitude("47.620691", AngleUnit.DEGREES));

    private static final String STOP_ID = "stop";
    private static final String STOP_NAME = "stop";
    private static final String EARLIEST_DAY_TRIP_ID = "earliest-trip";
    private static final String LATEST_DAY_TRIP_ID = "latest-trip";
    private static final String PRIOR_DAY_TRIP_ID = "prior-trip";
    private static final String ROUTE_ID = "route";
    private static final String ROUTE_NUMBER = "0";
    private static final String ROUTE_NAME = "Somewhere via Elsewhere";
    private static final String EARLIEST_DATE_SERVICE = "earliest";
    private static final String LATEST_DATE_SERVICE = "latest";
    private static final String PRIOR_DATE_SERVICE = "prior";
    private static final String NEVER_RUNNING_SERVICE = "never";

    private static final String ANOTHER_STOP_ID = "stop1";
    private static final String ANOTHER_STOP_NAME = "stop1";

    private static final LocalDateTime EARLIEST_TIME = LocalDateTime.of(
            2017, Month.APRIL, 3, 10, 30, 0);
    private static final LocalDateTime SAME_DAY_TIME = LocalDateTime.of(
            2017, Month.APRIL, 3, 23, 59, 0);
    private static final LocalDateTime LATEST_TIME = LocalDateTime.of(
            2017, Month.APRIL, 4, 1, 30, 0);

    private static final LocalDate EARLIEST_DATE = EARLIEST_TIME.toLocalDate();
    private static final LocalDate LATEST_DATE = LATEST_TIME.toLocalDate();
    private static final LocalDate PRIOR_DATE = EARLIEST_DATE.minusDays(1);

    private static final ServiceTypeCalendar CALENDAR
            = new PreloadedServiceTypeCalendar(
                    ImmutableMap.of(PRIOR_DATE,
                                    new ServiceSet(Collections.singleton(
                                            PRIOR_DATE_SERVICE)),
                                    EARLIEST_DATE,
                                    new ServiceSet(Collections.singleton(
                                            EARLIEST_DATE_SERVICE)),
                                    LATEST_DATE,
                                    new ServiceSet(Collections.singleton(
                                            LATEST_DATE_SERVICE))));

    @Test
    public void testFindsTripsForAllDaysWithMultiDay() throws Exception {
        final PreloadedStopTimesDirectory stops
                = new PreloadedStopTimesDirectory(ImmutableSet.of(
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0),
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 1)),
                                      new TripId(PRIOR_DAY_TRIP_ID)),
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0),
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 1)),
                                      new TripId(EARLIEST_DAY_TRIP_ID)),
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0),
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 1)),
                                      new TripId(LATEST_DAY_TRIP_ID))));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));

        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(EARLIEST_DAY_TRIP_ID),
                                new TripDetails(EARLIEST_DAY_TRIP_ID, ROUTE_ID,
                                                EARLIEST_DATE_SERVICE),
                                new TripGroupKey(LATEST_DAY_TRIP_ID),
                                new TripDetails(LATEST_DAY_TRIP_ID, ROUTE_ID,
                                                LATEST_DATE_SERVICE),
                                new TripGroupKey(PRIOR_DAY_TRIP_ID),
                                new TripDetails(PRIOR_DAY_TRIP_ID, ROUTE_ID,
                                                PRIOR_DATE_SERVICE)));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingTripScheduleCreator tripCreator
                = new DirectoryReadingTripScheduleCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        CALENDAR, stopIdMap);

        Assert.assertEquals(3, tripCreator.createTrips().size());
        Assert.assertTrue(stops.verify(ImmutableSet.of(
                new TimePair(new TransitTime(10, 30, 0),
                             new TransitTime(25, 30, 0)),
                new TimePair(new TransitTime(34, 30, 0),
                             new TransitTime(47, 59, 59)),
                new TimePair(new TransitTime(0, 0, 0),
                             new TransitTime(1, 30, 0)))));
    }

    @Test
    public void testFindsTripsForAllDaysWitSingleDay() throws Exception {
        final PreloadedStopTimesDirectory stops
                = new PreloadedStopTimesDirectory(ImmutableSet.of(
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0),
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 1)),
                                      new TripId(PRIOR_DAY_TRIP_ID)),
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0),
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 1)),
                                      new TripId(EARLIEST_DAY_TRIP_ID)),
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0),
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 1)),
                                      new TripId(LATEST_DAY_TRIP_ID))));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));

        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(EARLIEST_DAY_TRIP_ID),
                                new TripDetails(EARLIEST_DAY_TRIP_ID, ROUTE_ID,
                                                EARLIEST_DATE_SERVICE),
                                new TripGroupKey(LATEST_DAY_TRIP_ID),
                                new TripDetails(LATEST_DAY_TRIP_ID, ROUTE_ID,
                                                LATEST_DATE_SERVICE),
                                new TripGroupKey(PRIOR_DAY_TRIP_ID),
                                new TripDetails(PRIOR_DAY_TRIP_ID, ROUTE_ID,
                                                PRIOR_DATE_SERVICE)));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingTripScheduleCreator tripCreator
                = new DirectoryReadingTripScheduleCreator(
                        EARLIEST_TIME, SAME_DAY_TIME, stops, routes, trips,
                        CALENDAR, stopIdMap);

        Assert.assertEquals(2, tripCreator.createTrips().size());
        Assert.assertTrue(stops.verify(ImmutableSet.of(
                new TimePair(new TransitTime(10, 30, 0),
                             new TransitTime(23, 59, 0)),
                new TimePair(new TransitTime(34, 30, 0),
                             new TransitTime(47, 59, 0)))));
    }

    @Test
    public void testDoesNotCreateSingletons() throws Exception {
        final PreloadedStopTimesDirectory stops
                = new PreloadedStopTimesDirectory(ImmutableSet.of(
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0)),
                                      new TripId(PRIOR_DAY_TRIP_ID)),
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0)),
                                      new TripId(EARLIEST_DAY_TRIP_ID)),
                        new TripStops(ImmutableList.of(
                                new TripStop(new TransitTime(11, 30, 0),
                                             new TransitTime(11, 30, 0),
                                             STOP_ID, 0)),
                                      new TripId(LATEST_DAY_TRIP_ID))));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));

        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(EARLIEST_DAY_TRIP_ID),
                                new TripDetails(EARLIEST_DAY_TRIP_ID, ROUTE_ID,
                                                EARLIEST_DATE_SERVICE),
                                new TripGroupKey(LATEST_DAY_TRIP_ID),
                                new TripDetails(LATEST_DAY_TRIP_ID, ROUTE_ID,
                                                LATEST_DATE_SERVICE),
                                new TripGroupKey(PRIOR_DAY_TRIP_ID),
                                new TripDetails(PRIOR_DAY_TRIP_ID, ROUTE_ID,
                                                PRIOR_DATE_SERVICE)));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingTripScheduleCreator tripCreator
                = new DirectoryReadingTripScheduleCreator(
                        EARLIEST_TIME, SAME_DAY_TIME, stops, routes, trips,
                        CALENDAR, stopIdMap);

        Assert.assertTrue(tripCreator.createTrips().isEmpty());
    }

    @Test
    public void testDoesNotCreateEmpty() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                Collections.emptySet());
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(LATEST_DAY_TRIP_ID),
                                new TripDetails(LATEST_DAY_TRIP_ID, ROUTE_ID,
                                                NEVER_RUNNING_SERVICE)));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(ANOTHER_STOP_ID, ANOTHER_STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingTripScheduleCreator tripCreator
                = new DirectoryReadingTripScheduleCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        CALENDAR, stopIdMap);

        Assert.assertTrue(tripCreator.createTrips().isEmpty());
    }

    @Test
    public void testDoesNotCreateNotRunning() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                ImmutableSet.of(new TripStops(ImmutableList.of(
                        new TripStop(new TransitTime(0, 0, 0),
                                     new TransitTime(0, 0, 0), STOP_ID, 0),
                        new TripStop(new TransitTime(0, 0, 0),
                                     new TransitTime(0, 0, 0), STOP_ID, 1)),
                                              new TripId(LATEST_DAY_TRIP_ID))));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(LATEST_DAY_TRIP_ID),
                                new TripDetails(LATEST_DAY_TRIP_ID, ROUTE_ID,
                                                NEVER_RUNNING_SERVICE)));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(ANOTHER_STOP_ID, ANOTHER_STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingTripScheduleCreator tripCreator
                = new DirectoryReadingTripScheduleCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        CALENDAR, stopIdMap);

        Assert.assertTrue(tripCreator.createTrips().isEmpty());
    }

    @Test
    public void testDoesNotCreateOutOfMap() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                ImmutableSet.of(new TripStops(ImmutableList.of(
                        new TripStop(new TransitTime(0, 0, 0),
                                     new TransitTime(0, 0, 0), STOP_ID, 0),
                        new TripStop(new TransitTime(0, 0, 0),
                                     new TransitTime(0, 0, 0), STOP_ID, 1)),
                                              new TripId(LATEST_DAY_TRIP_ID))));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(LATEST_DAY_TRIP_ID),
                                new TripDetails(LATEST_DAY_TRIP_ID, ROUTE_ID,
                                                NEVER_RUNNING_SERVICE)));

        final Map<String, TransitStop> stopIdMap = ImmutableMap.of();

        final DirectoryReadingTripScheduleCreator tripCreator
                = new DirectoryReadingTripScheduleCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        CALENDAR, stopIdMap);

        Assert.assertTrue(tripCreator.createTrips().isEmpty());
    }

}
