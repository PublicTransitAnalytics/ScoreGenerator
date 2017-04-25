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

import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
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
public class RetrospectivePathTest {

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
    public void testMovesRideBackward() {

        final RetrospectivePath path = new RetrospectivePath();
        final MovementPath newPath = path.appendTransitRide(
                "tripId", "1", "Somewhere via Elsewhere", WAYPOINT_STOP,
                TIME_AT_WAYPOINT, ORIGIN_STOP, TIME_AT_ORIGIN);

        Assert.assertEquals(1, newPath.getMovements().size());
        final Movement movement = newPath.getMovements().get(0);
        Assert.assertEquals(TIME_AT_ORIGIN, movement.getStartTime());
        Assert.assertEquals(TIME_AT_WAYPOINT, movement.getEndTime());
    }

    @Test
    public void testMovesWalkBackward() {
        final RetrospectivePath path = new RetrospectivePath();
        final MovementPath newPath = path.appendWalk(
                WAYPOINT_STOP, TIME_AT_WAYPOINT, ORIGIN_STOP, TIME_AT_ORIGIN, 
                new WalkingCosts(Duration.ofSeconds(10), 10));

        Assert.assertEquals(1, newPath.getMovements().size());
        final Movement movement = newPath.getMovements().get(0);
        Assert.assertEquals(TIME_AT_ORIGIN, movement.getStartTime());
        Assert.assertEquals(TIME_AT_WAYPOINT, movement.getEndTime());
    }

    @Test
    public void testAppendsToFront() {
        final MovementPath path = new RetrospectivePath().appendWalk(
                WAYPOINT_STOP, TIME_AT_WAYPOINT, ORIGIN_STOP, TIME_AT_ORIGIN,
                new WalkingCosts(Duration.ofSeconds(10), 10));
        final Movement movement = path.getMovements().get(0);
        final MovementPath newPath = path.appendTransitRide(
                "tripId", "0", "Somewhere via Elsewhere", DESTINATION_STOP,
                TIME_AT_DESTINATION, WAYPOINT_STOP, TIME_AT_WAYPOINT);

        Assert.assertFalse(newPath.getMovements().get(0).equals(movement));
    }

    @Test
    public void testMakesNeøwPath() {
        final RetrospectivePath path = new RetrospectivePath();
        final MovementPath newPath = path.appendWalk(
                WAYPOINT_STOP, TIME_AT_WAYPOINT, ORIGIN_STOP, TIME_AT_ORIGIN,  
                new WalkingCosts(Duration.ofSeconds(10), 10));

        Assert.assertNotSame(newPath, path);
    }

}
