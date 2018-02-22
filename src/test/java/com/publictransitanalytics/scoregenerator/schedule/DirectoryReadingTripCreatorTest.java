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

import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedRouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedStopTimesDirectory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedTripDetailsDirectory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
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
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedScheduleInterpolator;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedServiceTypeCalendar;
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
public class DirectoryReadingTripCreatorTest {

    private final static GeoPoint POINT = new GeoPoint(
            new GeoLongitude("-122.33618", AngleUnit.DEGREES), 
            new GeoLatitude("47.620691", AngleUnit.DEGREES));

    private final String STOP_ID = "stop";
    private final String STOP_NAME = "stop";
    private final String TRIP_ID = "trip";
    private final String ROUTE_ID = "route";
    private final String ROUTE_NUMBER = "0";
    private final String ROUTE_NAME = "Somewhere via Elsewhere";
    private final String WEEKDAY_SERVICE = "weekday";
    private final String HOLIDAY_SERVICE = "holiday";

    private final String ANOTHER_STOP_ID = "stop1";
    private final String ANOTHER_STOP_NAME = "stop1";
    private final String ANOTHER_TRIP_ID = "trip1";

    private final LocalDateTime EARLIEST_TIME = LocalDateTime.of(
            2017, Month.APRIL, 3, 10, 30, 0);
    private final LocalDateTime LATEST_TIME = LocalDateTime.of(
            2017, Month.APRIL, 4, 1, 30, 0);

    private final LocalDate PRIOR_SERVICE_DATE
            = LocalDate.of(2017, Month.APRIL, 3);
    private final LocalDate SERVICE_DATE = LocalDate.of(2017, Month.APRIL, 4);
    
    private static final ScheduleInterpolatorFactory INTERPOLATOR_FACTORY 
            = (value) -> new PreloadedScheduleInterpolator(value);

    @Test
    public void testFindsTrip() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                Collections.singleton(new TripStop(
                        new TransitTime(0, 0, 0), STOP_ID, new TripId(TRIP_ID),
                        0)));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(TRIP_ID), new TripDetails(
                                TRIP_ID, ROUTE_ID, WEEKDAY_SERVICE)));
        final ServiceTypeCalendar calendar
                = new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        PRIOR_SERVICE_DATE,
                        new ServiceSet(Collections.singleton(WEEKDAY_SERVICE)),
                        SERVICE_DATE,
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE))));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingTripCreator tripCreator
                = new DirectoryReadingTripCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap, INTERPOLATOR_FACTORY);

        Assert.assertEquals(1, tripCreator.createTrips().size());

    }

    @Test
    public void testFindsTripAtIntervalEnd() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                Collections.singleton(new TripStop(
                        new TransitTime(1, 30, 0), STOP_ID, new TripId(TRIP_ID),
                        0)));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(TRIP_ID), new TripDetails(
                                TRIP_ID, ROUTE_ID, WEEKDAY_SERVICE)));
        final ServiceTypeCalendar calendar
                = new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        PRIOR_SERVICE_DATE,
                        new ServiceSet(Collections.singleton(WEEKDAY_SERVICE)),
                        SERVICE_DATE,
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE))));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingTripCreator tripCreator
                = new DirectoryReadingTripCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap, INTERPOLATOR_FACTORY);

        Assert.assertEquals(1, tripCreator.createTrips().size());

    }

    @Test
    public void testFindsAllTripAtStop() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                ImmutableSet.of(
                        new TripStop(new TransitTime(0, 0, 0), STOP_ID,
                                     new TripId(TRIP_ID), 0),
                        new TripStop(new TransitTime(0, 1, 0), STOP_ID,
                                     new TripId(ANOTHER_TRIP_ID), 0)));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(
                        new TripGroupKey(TRIP_ID), new TripDetails(
                                TRIP_ID, ROUTE_ID, WEEKDAY_SERVICE),
                        new TripGroupKey(ANOTHER_TRIP_ID), new TripDetails(
                                TRIP_ID, ROUTE_ID, WEEKDAY_SERVICE)));
        final ServiceTypeCalendar calendar
                = new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        PRIOR_SERVICE_DATE,
                        new ServiceSet(Collections.singleton(WEEKDAY_SERVICE)),
                        SERVICE_DATE,
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE))));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingTripCreator tripCreator
                = new DirectoryReadingTripCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap, INTERPOLATOR_FACTORY);

        Assert.assertEquals(2, tripCreator.createTrips().size());
    }

    @Test
    public void testDoesNotFoundNotRunning() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                ImmutableSet.of(
                        new TripStop(new TransitTime(0, 0, 0), STOP_ID,
                                     new TripId(TRIP_ID), 0),
                        new TripStop(new TransitTime(0, 1, 0), ANOTHER_STOP_ID,
                                     new TripId(TRIP_ID), 1)));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(TRIP_ID), new TripDetails(
                                TRIP_ID, ROUTE_ID, HOLIDAY_SERVICE)));
        final ServiceTypeCalendar calendar
                = new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        PRIOR_SERVICE_DATE,
                        new ServiceSet(Collections.singleton(WEEKDAY_SERVICE)),
                        SERVICE_DATE,
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE))));

        final TransitStop transitStop
                = new TransitStop(STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(ANOTHER_STOP_ID, ANOTHER_STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingTripCreator tripCreator
                = new DirectoryReadingTripCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap, INTERPOLATOR_FACTORY);

        Assert.assertTrue(tripCreator.createTrips().isEmpty());
    }

    @Test
    public void testDoesNotFindOutOfMap() throws Exception {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                ImmutableSet.of(
                        new TripStop(new TransitTime(0, 0, 0), STOP_ID,
                                     new TripId(TRIP_ID), 0),
                        new TripStop(new TransitTime(0, 1, 0), ANOTHER_STOP_ID,
                                     new TripId(TRIP_ID), 1)));
        final RouteDetailsDirectory routes
                = new PreloadedRouteDetailsDirectory(ImmutableMap.of(
                        ROUTE_ID, new RouteDetails(ROUTE_NUMBER, ROUTE_NAME)));
        final TripDetailsDirectory trips = new PreloadedTripDetailsDirectory(
                ImmutableMap.of(new TripGroupKey(TRIP_ID), new TripDetails(
                                TRIP_ID, ROUTE_ID, WEEKDAY_SERVICE)));
        final ServiceTypeCalendar calendar
                = new PreloadedServiceTypeCalendar(ImmutableMap.of(
                        PRIOR_SERVICE_DATE,
                        new ServiceSet(Collections.singleton(WEEKDAY_SERVICE)),
                        SERVICE_DATE,
                        new ServiceSet(Collections.singleton(
                                WEEKDAY_SERVICE))));

        final Map<String, TransitStop> stopIdMap = ImmutableMap.of();

        final DirectoryReadingTripCreator tripCreator
                = new DirectoryReadingTripCreator(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap, INTERPOLATOR_FACTORY);

        Assert.assertTrue(tripCreator.createTrips().isEmpty());
    }
    
}
