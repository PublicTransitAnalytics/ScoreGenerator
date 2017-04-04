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

import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.tracking.WalkMovement;
import com.publictransitanalytics.scoregenerator.tracking.ForwardMovingPath;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.TransitRideMovement;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.Assert;
import org.junit.Test;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
public class MovementPathTest {

    private static final Geodetic2DPoint ORIGIN_POINT = new Geodetic2DPoint(
            new Longitude(-122.3367105, Longitude.DEGREES),
            new Latitude(47.6072246, Latitude.DEGREES));

    private static final Geodetic2DPoint WAYPOINT = new Geodetic2DPoint(
            new Longitude(-122.3356412, Longitude.DEGREES),
            new Latitude(47.6154922, Latitude.DEGREES));

    private static final Geodetic2DPoint DESTINATION_POINT
            = new Geodetic2DPoint(new Longitude(-122.346473, Longitude.DEGREES),
                                  new Latitude(47.6135901, Latitude.DEGREES));

    @Test
    public void testEmptyPathDuration() {
        Assert.assertEquals(Duration.ZERO, new ForwardMovingPath(
                            ImmutableList.of()).getDuration());
    }

    @Test
    public void testGetsDuration() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 35),
                WAYPOINT);
        final Movement m2 = new TransitRideMovement(
                "tripId", "routeNumber", "routeName", "pickupStopId",
                "pickupStop", LocalDateTime.of(2017, Month.JANUARY, 23, 20, 36),
                "dropoffStopId", "dropoffStop",
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 39));
        final Movement m3 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 39),
                5.5, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                WAYPOINT);
        final MovementPath p1 = new ForwardMovingPath(
                ImmutableList.of()).makeAppended(m1).makeAppended(m2)
                .makeAppended(m3);
        Assert.assertEquals(Duration.ofMinutes(11), p1.getDuration());
    }

    @Test
    public void testEmptyBetter() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p1 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m1);

        final MovementPath p2 = new ForwardMovingPath(ImmutableList.of());

        Assert.assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    public void testEarlierBetter() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p1 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m1);

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                5, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 34),
                WAYPOINT);
        final Movement m3 = new TransitRideMovement(
                "tripId", "routeNumber", "routeName", "pickupStopId", 
                "pickupStop", LocalDateTime.of(2017, Month.JANUARY, 23, 20, 35),
                "dropoffStopId", "dropoffStop",
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 39));
        final MovementPath p2 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m2).makeAppended(m3);

        Assert.assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    public void testLessWalkingBetter() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p1 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m1);

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                5, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 34),
                WAYPOINT);
        final Movement m3 = new TransitRideMovement(
                "tripId", "routeNumber", "routeName", "pickupStopId", 
                "pickupStop", LocalDateTime.of(2017, Month.JANUARY, 23, 20, 35),
                "dropoffStopId", "dropoffStop",
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41));
        final MovementPath p2 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m2).makeAppended(m3);

        Assert.assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    public void testLessTimeBetter() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p1 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m1);

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 31),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 34),
                WAYPOINT);
        final Movement m3 = new TransitRideMovement(
                "tripId", "routeNumber", "routeName", "pickupStopId", 
                "pickupStop", LocalDateTime.of(2017, Month.JANUARY, 23, 20, 35),
                "dropoffStopId", "dropoffStop",
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41));
        final MovementPath p2 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m2).makeAppended(m3);

        Assert.assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    public void testFewerMovementsBetter() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p1 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m1);

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 34),
                WAYPOINT);
        final Movement m3 = new TransitRideMovement(
                "tripId", "routeNumber", "routeName", "pickupStopId", 
                "pickupStop", LocalDateTime.of(2017, Month.JANUARY, 23, 20, 35),
                "dropoffStopId", "dropoffStop",
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41));
        final MovementPath p2 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m2).makeAppended(m3);

        Assert.assertTrue(p2.compareTo(p1) > 0);
    }

    @Test
    public void testTiesBroken() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p1 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m1);

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);
        final MovementPath p2 = new ForwardMovingPath(ImmutableList.of())
                .makeAppended(m2);

        Assert.assertFalse(p1.compareTo(p2) == 0);
    }
}
