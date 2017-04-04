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

import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Landmark;
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
public class PointLocationTest {

    private final static Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));

    private final static Geodetic2DPoint POINT = new Geodetic2DPoint(
            new Longitude(-122.459696, Longitude.DEGREES), new Latitude(
                    47.734145, Latitude.DEGREES));

    @Test
    public void testNearestPoint() {
        final PointLocation pointLocation = new Landmark(SECTOR, POINT);
        Assert.assertEquals(POINT, pointLocation.getNearestPoint(
                            new Geodetic2DPoint()));
    }

    @Test
    public void testCanonicalPoint() {
        final PointLocation pointLocation = new Landmark(SECTOR, POINT);
        Assert.assertEquals(POINT, pointLocation.getCanonicalPoint());
    }
}
