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
public class RouteExtensionTest {

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

    @Test
    public void testExtendsMultiStopTripEnd() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_2, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<ScheduledLocation> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());
        final ScheduledLocation lastStop
                = newSchedule.get(newSchedule.size() - 1);
        Assert.assertEquals(STOP_3, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             55), lastStop.getScheduledTime());
    }

    @Test
    public void testMultiStopExtensionToTripEnd() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3),
                                 new RouteSequenceItem(Duration.ofMinutes(4),
                                                       STOP_2)));

        final List<ScheduledLocation> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());

        final ScheduledLocation nextStop = newSchedule.get(1);
        Assert.assertEquals(STOP_3, nextStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             45), nextStop.getScheduledTime());

        final ScheduledLocation lastStop
                = newSchedule.get(2);
        Assert.assertEquals(STOP_2, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             49), lastStop.getScheduledTime());
    }

    @Test
    public void testExtendsSingleStopTripEnd() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.AFTER_LAST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<ScheduledLocation> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final ScheduledLocation lastStop
                = newSchedule.get(newSchedule.size() - 1);
        Assert.assertEquals(STOP_3, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             45), lastStop.getScheduledTime());
    }

    @Test
    public void testDoesNotExtendMismatchedTrip() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));
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

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<ScheduledLocation> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());
        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             35), firstStop.getScheduledTime());
    }

    @Test
    public void testMultiStopExtensionToTripBeginning() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3),
                                 new RouteSequenceItem(Duration.ofMinutes(4),
                                                       STOP_2)));

        final List<ScheduledLocation> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(3, newSchedule.size());

        final ScheduledLocation previousStop = newSchedule.get(1);
        Assert.assertEquals(STOP_3, previousStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             35), previousStop.getScheduledTime());

        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_2, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             31), firstStop.getScheduledTime());
    }

    @Test
    public void testExtendsSingleStopTripBeginning() throws Exception {

        final Set<ScheduleEntry> route71Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final Trip route71 = new Trip(new TripId("71_1", LocalDate.MIN),
                                      "71", "71", route71Schedule);

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));

        final List<ScheduledLocation> newSchedule
                = extension.patch(route71).get().getSchedule();
        Assert.assertEquals(2, newSchedule.size());
        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             35), firstStop.getScheduledTime());
    }

    @Test
    public void testDoesNotChangeNonMatchingTrip() throws Exception {

        final Set<ScheduleEntry> route70Schedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));
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
                                      "71", "71", Collections.emptySet());

        final RouteExtension extension = new RouteExtension(
                "71", STOP_1, ReferenceDirection.BEFORE_FIRST,
                ImmutableList.of(new RouteSequenceItem(Duration.ofMinutes(5),
                                                       STOP_3)));
        final Trip modified = extension.patch(route71).get();

        Assert.assertEquals(route71, modified);
    }
}
