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

import com.publictransitanalytics.scoregenerator.schedule.DirectoryBackedTrip;
import com.publictransitanalytics.scoregenerator.schedule.ScheduledLocation;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedStopTimesDirectory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import java.time.Month;
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
public class DirectoryBackedTripTest {

    private static final String TRIP_ID_1 = "tripId1";

    private static final Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));

    private static final String STOP_ID_1 = "B";

    private static final TransitStop STOP_1 = new TransitStop(
            SECTOR, STOP_ID_1, "name1", new Geodetic2DPoint(
                    new Longitude(-122.324966, Longitude.DEGREES),
                    new Latitude(47.6647377, Latitude.DEGREES)));

    private static final String STOP_ID_0 = "C";

    private final static TransitStop STOP_0 = new TransitStop(
            SECTOR, STOP_ID_0, "name0", new Geodetic2DPoint(
                    new Longitude(-122.325676, Longitude.DEGREES),
                    new Latitude(47.6669731, Latitude.DEGREES)));

    private static final String STOP_ID_2 = "A";

    private final static TransitStop STOP_2 = new TransitStop(
            SECTOR, STOP_ID_2, "name2", new Geodetic2DPoint(
                    new Longitude(-122.325035, Longitude.DEGREES),
                    new Latitude(47.6592484, Latitude.DEGREES)));

    @Test
    public void testGetsLocationAtTime() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        Collections.singleton(tripStop)),
                ImmutableMap.of(STOP_ID_1, STOP_1));

        final LocalDateTime time = trip.getScheduledTime(STOP_1);
        Assert.assertEquals(LocalDateTime.of(2017, Month.JANUARY, 30, 1, 0, 0),
                            time);
    }

    @Test
    public void testGetsLocationAtOverflowedTime() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 25, (byte) 0, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        Collections.singleton(tripStop)),
                ImmutableMap.of(STOP_ID_1, STOP_1));

        final LocalDateTime time = trip.getScheduledTime(STOP_1);
        Assert.assertEquals(LocalDateTime.of(2017, Month.JANUARY, 30, 1, 0, 0),
                            time);
    }

    @Test
    public void testIgnoresUnmappedLocation() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        Collections.singleton(tripStop)),
               Collections.emptyMap());

        final LocalDateTime time = trip.getScheduledTime(STOP_1);
        Assert.assertNull(time);
    }

    @Test
    public void testIgnoresStopAfterTimeRange() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 30, (byte) 0), STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        Collections.singleton(tripStop)),
                ImmutableMap.of(STOP_ID_1, STOP_1));

        final LocalDateTime time = trip.getScheduledTime(STOP_1);
        Assert.assertNull(time);
    }

    @Test
    public void testIgnoresStopBeforeTimeRange() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 0, (byte) 30, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        Collections.singleton(tripStop)),
                ImmutableMap.of(STOP_ID_1, STOP_1));

        final LocalDateTime time = trip.getScheduledTime(STOP_1);
        Assert.assertNull(time);
    }

    @Test
    public void testGetsNext() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);
        final TripStop previousStop = new TripStop(
                new TransitTime((byte) 0, (byte) 59, (byte) 0),
                STOP_ID_0,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 2);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        ImmutableSet.of(previousStop, tripStop)),
                ImmutableMap.of(STOP_ID_0, STOP_0, STOP_ID_1, STOP_1));

        final ScheduledLocation scheduledLocation = trip
                .getNextScheduledLocation(STOP_0);
        final LocalDateTime time = scheduledLocation.getScheduledTime();
        final TransitStop stop = scheduledLocation.getLocation();
        Assert.assertEquals(LocalDateTime.of(2017, Month.JANUARY, 30, 1, 0, 0),
                            time);
        Assert.assertEquals(STOP_1, stop);
    }

    @Test
    public void testGetsPrevious() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 0), STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);
        final TripStop nextStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 3),
                STOP_ID_2,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 4);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        ImmutableSet.of(nextStop, tripStop)),
                ImmutableMap.of(STOP_ID_2, STOP_2, STOP_ID_1, STOP_1));

        final ScheduledLocation scheduledLocation = trip
                .getPreviousScheduledLocation(STOP_2);
        final LocalDateTime time = scheduledLocation.getScheduledTime();
        final TransitStop stop = scheduledLocation.getLocation();
        Assert.assertEquals(LocalDateTime.of(2017, Month.JANUARY, 30, 1, 0, 0),
                            time);
        Assert.assertEquals(STOP_1, stop);
    }

    @Test
    public void testGetsNoNextLocation() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        ImmutableSet.of(tripStop)),
                ImmutableMap.of(STOP_ID_1, STOP_1));

        final ScheduledLocation scheduledLocation = trip
                .getNextScheduledLocation(STOP_1);
        Assert.assertNull(scheduledLocation);
    }

    @Test
    public void testGetsNoPreviousLocation() {
        final TripStop tripStop = new TripStop(
                new TransitTime((byte) 1, (byte) 0, (byte) 0),
                STOP_ID_1,
                new com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId(
                        TRIP_ID_1), 3);

        final DirectoryBackedTrip trip = new DirectoryBackedTrip(
                new TripId(TRIP_ID_1), "Downtown Seattle", "1",
                LocalDateTime.of(2017, Month.JANUARY, 30, 0, 55, 0),
                LocalDateTime.of(2017, Month.JANUARY, 30, 1, 25, 0),
                new PreloadedStopTimesDirectory(
                        ImmutableSet.of(tripStop)),
                ImmutableMap.of(STOP_ID_1, STOP_1));

        final ScheduledLocation scheduledLocation = trip
                .getPreviousScheduledLocation(STOP_1);
        Assert.assertNull(scheduledLocation);
    }
}
