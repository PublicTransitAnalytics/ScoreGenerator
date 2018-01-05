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
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.ScheduleEntry;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
public class RouteRerouteTest {

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
    private final static TransitStop STOP_1 = new TransitStop(SECTOR, "stop1",
                                                              "stop1", POINT);
    private final static TransitStop STOP_2 = new TransitStop(SECTOR, "stop2",
                                                              "stop2", POINT);
    private final static TransitStop STOP_3 = new TransitStop(SECTOR, "stop3",
                                                              "stop3", POINT);
    private final static TransitStop STOP_4 = new TransitStop(SECTOR, "stop4",
                                                              "stop4", POINT);
    private final static TransitStop STOP_5 = new TransitStop(SECTOR, "stop5",
                                                              "stop5", POINT);
    private final static TransitStop STOP_6 = new TransitStop(SECTOR, "stop6",
                                                              "stop6", POINT);

    @Test
    public void testReroutesSingleStopTripEnd() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.AFTER_LAST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(5), STOP_3)),
                null, null);

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final ScheduledLocation firstStop
                = newSchedule.get(0);
        Assert.assertEquals(STOP_1, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 40),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_3, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 45),
                            secondStop.getScheduledTime());
    }

    @Test
    public void testNoRejoinForward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(3, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_3),
                new ScheduleEntry(4, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      57), STOP_5));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.AFTER_LAST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(3), STOP_2),
                        new RouteSequenceItem(Duration.ofMinutes(4), STOP_4)),
                null, null);

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_1, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 40),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_2, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 43),
                            secondStop.getScheduledTime());

        final ScheduledLocation thirdStop = newSchedule.get(2);
        Assert.assertEquals(STOP_4, thirdStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 47),
                            thirdStop.getScheduledTime());
    }

    @Test
    public void testRejoinsRouteForward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      45), STOP_2),
                new ScheduleEntry(3, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_3),
                new ScheduleEntry(4, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      57), STOP_4));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.AFTER_LAST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(3), STOP_5),
                        new RouteSequenceItem(Duration.ofMinutes(1), STOP_6)),
                STOP_3, Duration.ofMinutes(2));

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(5, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_1, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 40),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_5, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 43),
                            secondStop.getScheduledTime());

        final ScheduledLocation thirdStop = newSchedule.get(2);
        Assert.assertEquals(STOP_6, thirdStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 44),
                            thirdStop.getScheduledTime());

        final ScheduledLocation fourthStop = newSchedule.get(3);
        Assert.assertEquals(STOP_3, fourthStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 46),
                            fourthStop.getScheduledTime());

        final ScheduledLocation fifthStop = newSchedule.get(4);
        Assert.assertEquals(STOP_4, fifthStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 53),
                            fifthStop.getScheduledTime());
    }

    @Test
    public void testNoRejoinPointForward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      45), STOP_2),
                new ScheduleEntry(3, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_3));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.AFTER_LAST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(3), STOP_5)),
                STOP_4, Duration.ofMinutes(2));

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_1, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 40),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_5, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 43),
                            secondStop.getScheduledTime());
    }

    @Test
    public void testNoDivergenceForward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_2, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(
                        Duration.ofMinutes(3), STOP_4)), null, null);

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(1, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_1, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 40),
                            firstStop.getScheduledTime());
    }

    @Test
    public void testReroutesSingleStopTripBeginning() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)), null, null);

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_1, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 40),
                            secondStop.getScheduledTime());
    }

    @Test
    public void testReroutesSingleStopTripBegin() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)), null, null);

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             35), firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_1, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             40), secondStop.getScheduledTime());
    }

    @Test
    public void testNoRejoinBackward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(3, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_3),
                new ScheduleEntry(4, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      57), STOP_5));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_3, ReferenceDirection.BEFORE_FIRST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(1), STOP_2),
                        new RouteSequenceItem(Duration.ofMinutes(3), STOP_4)),
                null, null);

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(4, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_4, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 46),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_2, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 49),
                            secondStop.getScheduledTime());

        final ScheduledLocation thirdStop = newSchedule.get(2);
        Assert.assertEquals(STOP_3, thirdStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 50),
                            thirdStop.getScheduledTime());

        final ScheduledLocation fourthStop = newSchedule.get(3);
        Assert.assertEquals(STOP_5, fourthStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 57),
                            fourthStop.getScheduledTime());
    }

    @Test
    public void testRejoinsRouteBackward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(3, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_3),
                new ScheduleEntry(4, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      56), STOP_5),
                new ScheduleEntry(6, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      57), STOP_6));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_6, ReferenceDirection.BEFORE_FIRST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(1), STOP_2),
                        new RouteSequenceItem(Duration.ofMinutes(3), STOP_4)),
                STOP_3, Duration.ofMinutes(4));

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(5, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_1, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 39),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_3, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 49),
                            secondStop.getScheduledTime());

        final ScheduledLocation thirdStop = newSchedule.get(2);
        Assert.assertEquals(STOP_4, thirdStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 53),
                            thirdStop.getScheduledTime());

        final ScheduledLocation fourthStop = newSchedule.get(3);
        Assert.assertEquals(STOP_2, fourthStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 56),
                            fourthStop.getScheduledTime());

        final ScheduledLocation fifthStop = newSchedule.get(4);
        Assert.assertEquals(STOP_6, fifthStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 57),
                            fifthStop.getScheduledTime());
    }

    @Test
    public void testNoRejoinPointBackward() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(4, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      56), STOP_5),
                new ScheduleEntry(6, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      57), STOP_6));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_6, ReferenceDirection.BEFORE_FIRST, ImmutableList.of(
                        new RouteSequenceItem(Duration.ofMinutes(1), STOP_2),
                        new RouteSequenceItem(Duration.ofMinutes(3), STOP_4)),
                STOP_3, Duration.ofMinutes(4));

        final List<ScheduledLocation> newSchedule
                = reroute.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_4, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 53),
                            firstStop.getScheduledTime());

        final ScheduledLocation secondStop = newSchedule.get(1);
        Assert.assertEquals(STOP_2, secondStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 56),
                            secondStop.getScheduledTime());

        final ScheduledLocation thirdStop = newSchedule.get(2);
        Assert.assertEquals(STOP_6, thirdStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 57),
                            thirdStop.getScheduledTime());
    }

    @Test
    public void testDoesNotChangeEmptyTrip() throws Exception {

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", Collections.emptySet());

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)), null, null);
        final Trip modified = reroute.patch(route71).get();

        Assert.assertEquals(route71, modified);
    }

    @Test
    public void testDoesNotChangeNonMatchingTrip() throws Exception {

        final Set<ScheduleEntry> route70Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));
        final Trip route70 = new Trip(new TripId("70_1", LocalDate.MIN),
                                      "70", "70", route70Schedule);

        final RouteReroute reroute = new RouteReroute(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)), null, null);
        final Trip modified = reroute.patch(route70).get();
        Assert.assertEquals(route70, modified);
    }
}