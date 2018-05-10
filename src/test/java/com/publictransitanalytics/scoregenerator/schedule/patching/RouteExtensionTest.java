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
package com.publictransitanalytics.scoregenerator.schedule.patching;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class RouteExtensionTest {

    private final static GeoPoint POINT = new GeoPoint(
            new GeoLongitude("-122.33618", AngleUnit.DEGREES),
            new GeoLatitude("47.620691", AngleUnit.DEGREES));
    private final static TransitStop STOP_1 = new TransitStop("stop1",
                                                              "stop1", POINT);
    private final static TransitStop STOP_2 = new TransitStop("stop2",
                                                              "stop2", POINT);
    private final static TransitStop STOP_3 = new TransitStop("stop3",
                                                              "stop3", POINT);

    @Test
    public void testExtendsMultiStopTripEnd() throws Exception {

        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final LocalDateTime time2 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 50);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1),
                new VehicleEvent(STOP_2, time2, time2));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_2, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<VehicleEvent> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());
        final VehicleEvent lastStop
                = newSchedule.get(newSchedule.size() - 1);
        Assert.assertEquals(STOP_3, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 55),
                            lastStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 55),
                            lastStop.getDepartureTime());
    }

    @Test
    public void testMultiStopExtensionToTripEnd() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3),
                                 new RouteSequenceItem(Duration.ofMinutes(4),
                                                       STOP_2)));

        final List<VehicleEvent> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());

        final VehicleEvent nextStop = newSchedule.get(1);
        Assert.assertEquals(STOP_3, nextStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 45),
                            nextStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 45),
                            nextStop.getDepartureTime());

        final VehicleEvent lastStop
                = newSchedule.get(2);
        Assert.assertEquals(STOP_2, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 49),
                            lastStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 49),
                            lastStop.getDepartureTime());
    }

    @Test
    public void testExtendsSingleStopTripEnd() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<VehicleEvent> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final VehicleEvent lastStop
                = newSchedule.get(newSchedule.size() - 1);
        Assert.assertEquals(STOP_3, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 45),
                            lastStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 45),
                            lastStop.getDepartureTime());
    }

    @Test
    public void testDoesNotExtendMismatchedTrip() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1));
        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_2, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));
        final Trip extended = extension.patch(route71).get();

        Assert.assertEquals(route71, extended);

    }

    @Test
    public void testExtendsMultiStopTripBeginning() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final LocalDateTime time2 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 50);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1),
                new VehicleEvent(STOP_2, time2, time2));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<VehicleEvent> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());
        final VehicleEvent firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            firstStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            firstStop.getDepartureTime());
    }

    @Test
    public void testMultiStopExtensionToTripBeginning() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3),
                                 new RouteSequenceItem(Duration.ofMinutes(4),
                                                       STOP_2)));

        final List<VehicleEvent> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());

        final VehicleEvent previousStop = newSchedule.get(1);
        Assert.assertEquals(STOP_3, previousStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            previousStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            previousStop.getDepartureTime());

        final VehicleEvent firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_2, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 31),
                            firstStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 31),
                            firstStop.getDepartureTime());
    }

    @Test
    public void testExtendsSingleStopTripBeginning() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final List<VehicleEvent> route71Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<VehicleEvent> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());
        final VehicleEvent firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            firstStop.getArrivalTime());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            firstStop.getDepartureTime());
    }

    @Test
    public void testDoesNotChangeNonMatchingTrip() throws Exception {
        final LocalDateTime time1 = LocalDateTime.of(
                2017, Month.OCTOBER, 3, 9, 40);
        final List<VehicleEvent> route70Schedule = ImmutableList.of(
                new VehicleEvent(STOP_1, time1, time1));
        final Trip route70 = new Trip(new TripId("70_1", LocalDate.MIN),
                                      "70", "70", route70Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));
        final Trip modified = extension.patch(route70).get();
        Assert.assertEquals(route70, modified);
    }

    @Test
    public void testDoesNotChangeEmptyTrip() throws Exception {

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", Collections.emptyList());

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));
        final Trip modified = extension.patch(route71).get();

        Assert.assertEquals(route71, modified);
    }
}
