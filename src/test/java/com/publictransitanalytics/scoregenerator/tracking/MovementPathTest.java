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
package com.publictransitanalytics.scoregenerator.tracking;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
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
public class MovementPathTest {

    private final static Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));
    private static final Geodetic2DPoint ORIGIN_POINT = new Geodetic2DPoint(
            new Longitude(-122.3367105, Longitude.DEGREES),
            new Latitude(47.6072246, Latitude.DEGREES));
    private static final TransitStop ORIGIN_STOP = new TransitStop(
            SECTOR, "originStopId", "originStopName", ORIGIN_POINT);
    private static final LocalDateTime TIME_AT_ORIGIN
            = LocalDateTime.of(2017, Month.JANUARY, 23, 20, 20);

    private static final Geodetic2DPoint WAYPOINT_POINT = new Geodetic2DPoint(
            new Longitude(-122.3356412, Longitude.DEGREES),
            new Latitude(47.6154922, Latitude.DEGREES));
    private static final TransitStop WAYPOINT_STOP = new TransitStop(
            SECTOR, "waypointStopId", "waypointStopName", WAYPOINT_POINT);
    private static final LocalDateTime TIME_AT_WAYPOINT
            = LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30);

    private static final Geodetic2DPoint DESTINATION_POINT
            = new Geodetic2DPoint(new Longitude(-122.346473, Longitude.DEGREES),
                                  new Latitude(47.6135901, Latitude.DEGREES));
    private static final TransitStop DESTINATION_STOP = new TransitStop(
            SECTOR, "waypointStopId", "waypointStopName", DESTINATION_POINT);
    private static final LocalDateTime TIME_AT_DESTINATION
            = LocalDateTime.of(2017, Month.JANUARY, 23, 21, 10);

    @Test
    public void testEmptyPathDuration() {
        Assert.assertEquals(Duration.ZERO, new ForwardMovingPath(
                            ImmutableList.of()).getDuration());
    }

    @Test
    public void testGetsDuration() {
        final MovementPath path = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, WAYPOINT_STOP, TIME_AT_WAYPOINT)
                .appendTransitRide(
                        "tripId", "0", "Somewhere via Elsewhere", WAYPOINT_STOP,
                        TIME_AT_WAYPOINT, DESTINATION_STOP,
                        TIME_AT_DESTINATION);

        Assert.assertEquals(Duration.ofMinutes(50), path.getDuration());
    }

    @Test
    public void testEmptyBetter() {
        final MovementPath emptyPath = new ForwardMovingPath();

        final MovementPath nonemptyPath = emptyPath.appendTransitRide(
                "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                TIME_AT_ORIGIN, WAYPOINT_STOP, TIME_AT_WAYPOINT);

        Assert.assertTrue(nonemptyPath.compareTo(emptyPath) > 0);
    }

    @Test
    public void testEarlierArrivalBetter() {
        final MovementPath earlierPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, WAYPOINT_STOP, TIME_AT_WAYPOINT)
                .appendTransitRide(
                        "tripId", "0", "Somewhere via Elsewhere", WAYPOINT_STOP,
                        TIME_AT_WAYPOINT, DESTINATION_STOP,
                        TIME_AT_DESTINATION);

        final MovementPath laterPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP,
                        LocalDateTime.of(2017, Month.JANUARY, 23, 21, 11));

        Assert.assertTrue(laterPath.compareTo(earlierPath) > 0);
    }

    @Test
    public void testLaterStartBetter() {
        final MovementPath earlierPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        LocalDateTime.of(2017, Month.JANUARY, 23, 20, 10),
                        WAYPOINT_STOP, TIME_AT_WAYPOINT)
                .appendTransitRide(
                        "tripId", "0", "Somewhere via Elsewhere", WAYPOINT_STOP,
                        TIME_AT_WAYPOINT, DESTINATION_STOP,
                        TIME_AT_DESTINATION);

        final MovementPath laterPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP, TIME_AT_DESTINATION);

        Assert.assertTrue(earlierPath.compareTo(laterPath) > 0);
    }

    @Test
    public void testLessWalkingBetter() {
        final MovementPath noWalkingPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, WAYPOINT_STOP, TIME_AT_WAYPOINT)
                .appendTransitRide(
                        "tripId", "0", "Somewhere via Elsewhere", WAYPOINT_STOP,
                        TIME_AT_WAYPOINT, DESTINATION_STOP,
                        TIME_AT_DESTINATION);

        final MovementPath walkingPath = new ForwardMovingPath().appendWalk(
                ORIGIN_STOP, TIME_AT_ORIGIN, DESTINATION_STOP,
                TIME_AT_DESTINATION,
                new WalkingCosts(Duration.ofSeconds(50), 10));

        Assert.assertTrue(walkingPath.compareTo(noWalkingPath) > 0);
    }

    @Test
    public void testFewerMovementsBetter() {
        final MovementPath twoModePath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, WAYPOINT_STOP, TIME_AT_WAYPOINT)
                .appendTransitRide(
                        "tripId", "0", "Somewhere via Elsewhere", WAYPOINT_STOP,
                        TIME_AT_WAYPOINT, DESTINATION_STOP,
                        TIME_AT_DESTINATION);

        final MovementPath oneModePath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP, TIME_AT_DESTINATION);

        Assert.assertTrue(twoModePath.compareTo(oneModePath) > 0);
    }

    @Test
    public void testEquals() {

        final MovementPath aPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP, TIME_AT_DESTINATION);

        final MovementPath otherPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP, TIME_AT_DESTINATION);

        Assert.assertTrue(aPath.compareTo(otherPath) == 0);
    }

    @Test
    public void testTiesBroken() {

        final MovementPath aPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripId", "1", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP, TIME_AT_DESTINATION);

        final MovementPath otherPath = new ForwardMovingPath()
                .appendTransitRide(
                        "tripIdX", "1X", "Somewhere via Elsewhere", ORIGIN_STOP,
                        TIME_AT_ORIGIN, DESTINATION_STOP, TIME_AT_DESTINATION);

        Assert.assertFalse(aPath.compareTo(otherPath) == 0);
    }
}
