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
package com.publictransitanalytics.scoregenerator.location;

import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.tracking.ForwardMovingPath;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.tracking.WalkMovement;
import com.google.common.collect.ImmutableList;
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
public class VisitableLocationTest {

    private final static Sector SECTOR = new Sector(
            new Geodetic2DBounds(
                    new Geodetic2DPoint(
                            new Longitude(-122.459696, Longitude.DEGREES),
                            new Latitude(47.734145, Latitude.DEGREES)),
                    new Geodetic2DPoint(
                            new Longitude(-122.224433, Longitude.DEGREES),
                            new Latitude(47.48172, Latitude.DEGREES))));

    private final static Geodetic2DPoint POINT = new Geodetic2DPoint(
            new Longitude(-122.318971, Longitude.DEGREES), new Latitude(
                    47.548922, Latitude.DEGREES));

    private final static Geodetic2DPoint ORIGIN_POINT = new Geodetic2DPoint(
            new Longitude(-122.3196685, Longitude.DEGREES), new Latitude(
                    47.5489238, Latitude.DEGREES));

    private final LocalDateTime TIME = LocalDateTime
            .of(1987, Month.MARCH, 8, 16, 39);

    @Test
    public void testAddPath() {
        final VisitableLocation location = new Landmark(SECTOR, POINT);

        final MovementPath path = new ForwardMovingPath(ImmutableList.of());
        location.addPath(TIME, path);
        Assert.assertNotNull(location.getPaths().get(TIME));
        Assert.assertEquals(1, location.getPaths().get(TIME).size());
        Assert.assertTrue(location.getPaths().get(TIME).contains(path));
    }

    @Test
    public void testAnyPathBetterThanNone() {
        final VisitableLocation location = new Landmark(SECTOR, POINT);

        final MovementPath path = new ForwardMovingPath(ImmutableList.of());
        Assert.assertTrue(location.hasNoBetterPath(TIME, path));
    }

    @Test
    public void testSamePathNotBetter() {
        final VisitableLocation location = new Landmark(SECTOR, POINT);
        location.addPath(TIME, new ForwardMovingPath(ImmutableList.of()));

        final MovementPath path = new ForwardMovingPath(ImmutableList.of());
        Assert.assertFalse(location.hasNoBetterPath(TIME, path));
    }

    @Test
    public void testBetterPathBetter() {
        final VisitableLocation location = new Landmark(SECTOR, POINT);
        location.addPath(
                TIME, new ForwardMovingPath(
                        ImmutableList.of()).makeAppended(new WalkMovement(
                        LocalDateTime.of(1987, Month.MARCH, 8, 16, 39), 70,
                        ORIGIN_POINT,
                        LocalDateTime.of(1987, Month.MARCH, 8, 16, 49),
                        POINT)));

        final MovementPath path = new ForwardMovingPath(ImmutableList.of());
        Assert.assertTrue(location.hasNoBetterPath(TIME, path));
    }
}
