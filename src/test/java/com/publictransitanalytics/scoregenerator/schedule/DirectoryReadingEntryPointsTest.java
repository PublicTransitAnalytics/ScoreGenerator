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
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedServiceTypeCalendar;
import java.time.LocalDate;
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
public class DirectoryReadingEntryPointsTest {

    private final static Sector SECTOR = new Sector(
            new Geodetic2DBounds(
                    new Geodetic2DPoint(
                            new Longitude(-122.459696, Longitude.DEGREES),
                            new Latitude(47.734145, Latitude.DEGREES)),
                    new Geodetic2DPoint(
                            new Longitude(-122.224433, Longitude.DEGREES),
                            new Latitude(47.48172, Latitude.DEGREES))));

    private final static Geodetic2DPoint POINT = new Geodetic2DPoint(
            new Longitude(-122.3361768, Longitude.DEGREES), new Latitude(
                    47.6206914, Latitude.DEGREES));

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

    @Test
    public void testFindsTrip() {
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
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, LATEST_TIME).size());

    }

    @Test
    public void testFindsTripAtIntervalEnd() {
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
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, LATEST_TIME).size());

    }

    @Test
    public void testFindsAllTripAtStop() {
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
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertEquals(2, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, LATEST_TIME).size());
    }

    @Test
    public void testFindsAllTripStops() {
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

        final TransitStop transitStop
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(SECTOR, ANOTHER_STOP_ID, ANOTHER_STOP_NAME,
                                  POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, LATEST_TIME).size());
        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            anotherTransitStop, EARLIEST_TIME, LATEST_TIME)
                            .size());
    }

    @Test
    public void testDoesNotFoundNotRunning() {
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
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(SECTOR, ANOTHER_STOP_ID, ANOTHER_STOP_NAME,
                                  POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertTrue(entryPoints.getEntryPoints(
                transitStop, EARLIEST_TIME, LATEST_TIME).isEmpty());
        Assert.assertTrue(entryPoints.getEntryPoints(
                anotherTransitStop, EARLIEST_TIME, LATEST_TIME).isEmpty());
    }

    @Test
    public void testDoesNotFindOutOfMap() {
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

        final TransitStop transitStop
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(SECTOR, ANOTHER_STOP_ID, ANOTHER_STOP_NAME,
                                  POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, LATEST_TIME).size());
        Assert.assertTrue(entryPoints.getEntryPoints(
                anotherTransitStop, EARLIEST_TIME, LATEST_TIME).isEmpty());
    }

    @Test
    public void testDoesNotFindOutOfScheduleTime() {
        final StopTimesDirectory stops = new PreloadedStopTimesDirectory(
                ImmutableSet.of(
                        new TripStop(new TransitTime(0, 0, 0), STOP_ID,
                                     new TripId(TRIP_ID), 0),
                        new TripStop(new TransitTime(2, 1, 0), ANOTHER_STOP_ID,
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

        final TransitStop transitStop
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(SECTOR, ANOTHER_STOP_ID, ANOTHER_STOP_NAME,
                                  POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);

        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, LATEST_TIME).size());
        Assert.assertTrue(entryPoints.getEntryPoints(
                anotherTransitStop, EARLIEST_TIME, LATEST_TIME).isEmpty());
    }

    @Test
    public void testDoesNotFindOutOfRequestTime() {
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

        final TransitStop transitStop
                = new TransitStop(SECTOR, STOP_ID, STOP_NAME, POINT);
        final TransitStop anotherTransitStop
                = new TransitStop(SECTOR, ANOTHER_STOP_ID, ANOTHER_STOP_NAME,
                                  POINT);

        final Map<String, TransitStop> stopIdMap
                = ImmutableMap.of(STOP_ID, transitStop,
                                  ANOTHER_STOP_ID, anotherTransitStop);

        final DirectoryReadingEntryPoints entryPoints
                = new DirectoryReadingEntryPoints(
                        EARLIEST_TIME, LATEST_TIME, stops, routes, trips,
                        calendar, stopIdMap);
        final LocalDateTime queryTime = LocalDateTime.of(
                2017, Month.APRIL, 4, 0, 0, 0);

        Assert.assertEquals(1, entryPoints.getEntryPoints(
                            transitStop, EARLIEST_TIME, queryTime).size());
        Assert.assertTrue(entryPoints.getEntryPoints(
                anotherTransitStop, EARLIEST_TIME, queryTime).isEmpty());

    }

}
