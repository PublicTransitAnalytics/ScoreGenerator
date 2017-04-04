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
import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRider;
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class RetrospectiveScheduleReaderTest {
    
//        @Test
//    public void testFindsScheduled() {
//
//        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
//                STOP_TIME, new PreloadedTrip(
//                        new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), "Somewhere via Elsewhere",
//                        ROUTE_NUMBER, ImmutableList.of(new ScheduledLocation(
//                                STOP_ON_TRIP, STOP_TIME)))));
//
//        final LocalSchedule schedule = new PreloadedLocalSchedule(path);
//
//        final RetrospectiveRider rider = new RetrospectiveRider(
//                STOP_ON_TRIP, schedule,
//                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 15, 0));
//        final Set<RiderStatus> trips = rider.getEntryPoints(
//                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 45, 0));
//        Assert.assertEquals(1, trips.size());
//    }
//
//    @Test
//    public void testDoesNotFindOutsideOfWindow() {
//
//        final Map<LocalDateTime, Trip> path = new TreeMap<>(ImmutableMap.of(
//                STOP_TIME, new PreloadedTrip(
//                        new TripId(TRIP_BASE_ID, TRIP_SERVICE_DAY), ROUTE_NAME,
//                        ROUTE_NUMBER, ImmutableList.of(new ScheduledLocation(
//                                STOP_ON_TRIP, STOP_TIME)))));
//
//        final LocalSchedule schedule = new PreloadedLocalSchedule(path);
//
//        final RetrospectiveRider rider = new RetrospectiveRider(
//                STOP_ON_TRIP, schedule,
//                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 35, 0));
//        final Set<RiderStatus> trips = rider.getEntryPoints(
//                LocalDateTime.of(2017, Month.FEBRUARY, 12, 10, 45, 0));
//        Assert.assertTrue(trips.isEmpty());
//    }
    
}
