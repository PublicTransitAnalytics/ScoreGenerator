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
package com.bitvantage.seattletransitisochrone.types.tracking;

import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.tracking.RetrospectivePath;
import com.publictransitanalytics.scoregenerator.tracking.WalkMovement;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.google.common.collect.ImmutableList;
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
public class RetrospectivePathTest {

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
    public void testAppendsToFront() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, WAYPOINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);

        final RetrospectivePath path = new RetrospectivePath(
                ImmutableList.of(m1));

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 20),
                10, ORIGIN_POINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                WAYPOINT);

        final MovementPath newPath = path.makeAppended(m2);
        Assert.assertEquals(m2, newPath.getMovements().get(0));
    }

    @Test
    public void testNewList() {
        final Movement m1 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                10, WAYPOINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 41),
                DESTINATION_POINT);

        final RetrospectivePath path = new RetrospectivePath(
                ImmutableList.of(m1));

        final Movement m2 = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 20),
                10, WAYPOINT,
                LocalDateTime.of(2017, Month.JANUARY, 23, 20, 30),
                DESTINATION_POINT);

        final MovementPath newPath = path.makeAppended(m2);
        Assert.assertNotSame(path.getMovements(), newPath.getMovements());
    }

}
