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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedTransitNetwork;
import edu.emory.mathcs.backport.java.util.Collections;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
public class PatchingTripCreatorTest {

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
    public void testRemovesTrips() throws Exception {
        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptySet());
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(trip70_1,
                                new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", Collections.emptySet()),
                                new Trip(new TripId("71_2", LocalDate.MIN),
                                         "71", "71", Collections.emptySet())));

        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.singleton("71"), ArrayListMultimap.create(),
                baseNetwork);
        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(1, trips.size());
        Assert.assertEquals(trip70_1, trips.iterator().next());
    }

    @Test
    public void testExtendsMultiStopTripEnd() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", route71)));

        final RouteExtension extension = new RouteExtension(
                STOP_2, ExtensionType.AFTER_LAST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(),
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        final List<ScheduledLocation> newSchedule
                = trips.iterator().next().getSchedule();
        Assert.assertEquals(3, newSchedule.size());
        final ScheduledLocation lastStop
                = newSchedule.get(newSchedule.size() - 1);
        Assert.assertEquals(STOP_3, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             55), lastStop.getScheduledTime());
    }

    @Test
    public void testExtendsSingleStopTripEnd() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", route71)));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.AFTER_LAST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        final List<ScheduledLocation> newSchedule
                = trips.iterator().next().getSchedule();
        Assert.assertEquals(2, newSchedule.size());

        final ScheduledLocation lastStop
                = newSchedule.get(newSchedule.size() - 1);
        Assert.assertEquals(STOP_3, lastStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             45), lastStop.getScheduledTime());
    }

    @Test
    public void testDoesNotExtendSingleStopTrip() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));
        final Trip route71Trip = new Trip(new TripId("71_1", LocalDate.MIN),
                                          "71", "71", route71);
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(route71Trip));

        final RouteExtension extension = new RouteExtension(
                STOP_2, ExtensionType.AFTER_LAST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(1, trips.size());
        Assert.assertTrue(trips.contains(route71Trip));

    }

    @Test
    public void testExtendsMultiStopTripBeginning() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", route71)));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.BEFORE_FIRST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        final List<ScheduledLocation> newSchedule
                = trips.iterator().next().getSchedule();
        Assert.assertEquals(3, newSchedule.size());
        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             35), firstStop.getScheduledTime());
    }

    @Test
    public void testExtendsSingleStopTripBeginning() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1));

        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", route71)));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.BEFORE_FIRST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        final List<ScheduledLocation> newSchedule
                = trips.iterator().next().getSchedule();
        Assert.assertEquals(2, newSchedule.size());
        final ScheduledLocation firstStop = newSchedule.get(0);
        Assert.assertEquals(STOP_3, firstStop.getLocation());
        Assert.assertEquals(LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                             35), firstStop.getScheduledTime());
    }

    @Test
    public void testChangesAllTrips() throws Exception {

        final Set<ScheduleEntry> route71_1 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 8,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 8,
                                                      50), STOP_2));
        final Set<ScheduleEntry> route71_2 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", route71_1),
                                new Trip(new TripId("71_2", LocalDate.MIN),
                                         "71", "71", route71_2)));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.BEFORE_FIRST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        final Set<LocalDateTime> firstTimes = trips.stream()
                .map(trip -> trip.getSchedule().get(0).getScheduledTime())
                .collect(Collectors.toSet());
        Assert.assertEquals(ImmutableSet.of(
                LocalDateTime.of(2017, Month.OCTOBER, 3, 8, 35),
                LocalDateTime.of(2017, Month.OCTOBER, 3, 9, 35)), firstTimes);
    }

    @Test
    public void testChangesChangesSpecifiedDirection() throws Exception {

        final Set<ScheduleEntry> route71IbSchedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 8,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 8,
                                                      50), STOP_2));
        final Trip route71Ib = new Trip(new TripId(
                "71_ib", LocalDate.MIN), "71", "71", route71IbSchedule);
        final Set<ScheduleEntry> route71ObSchedule = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_2),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_1));
        final Trip route71Ob = new Trip(new TripId("71_ob", LocalDate.MIN),
                                        "71", "71", route71ObSchedule);

        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(route71Ib, route71Ob));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.BEFORE_FIRST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(2, trips.size());
        Assert.assertTrue(trips.contains(route71Ob));
        Assert.assertFalse(trips.contains(route71Ib));
    }

    @Test
    public void testDoesNotChangeOtherTrip() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptySet());
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_1", LocalDate.MIN),
                                         "71", "71", route71), trip70_1));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.BEFORE_FIRST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(2, trips.size());
        Assert.assertTrue(trips.contains(trip70_1));
    }

    @Test
    public void testDoesNotChangeEmptyTrip() throws Exception {

        final Set<ScheduleEntry> route71 = ImmutableSet.of(
                new ScheduleEntry(1, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      40), STOP_1),
                new ScheduleEntry(2, LocalDateTime.of(2017, Month.OCTOBER, 3, 9,
                                                      50), STOP_2));

        final Trip trip71_1 = new Trip(new TripId("71_1", LocalDate.MIN),
                                       "71", "71", Collections.emptySet());
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(new Trip(new TripId("71_2", LocalDate.MIN),
                                         "71", "71", route71), trip71_1));

        final RouteExtension extension = new RouteExtension(
                STOP_1, ExtensionType.BEFORE_FIRST,
                ImmutableSortedMap.of(Duration.ofMinutes(5), STOP_3));
        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.emptySet(), 
                ImmutableListMultimap.of("71", extension), baseNetwork);

        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(2, trips.size());
        Assert.assertTrue(trips.contains(trip71_1));
    }

}
