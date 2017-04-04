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
import com.publictransitanalytics.scoregenerator.testhelpers.CountingVisitor;
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
public class SectorTest {

    private final static Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));
    final static Geodetic2DPoint CENTER = new Geodetic2DPoint(
            new Longitude(-122.342064, Longitude.DEGREES),
            new Latitude(47.607932, Latitude.DEGREES));

    @Test
    public void testContains() {
        Assert.assertTrue(SECTOR.contains(new Geodetic2DPoint(
                new Longitude(-122.2986405, Latitude.DEGREES),
                new Latitude(47.6081435, Longitude.DEGREES))));
    }

    @Test
    public void testContainsCorner() {
        Assert.assertTrue(SECTOR.contains(new Geodetic2DPoint(
                new Longitude(-122.459696, Longitude.DEGREES),
                new Latitude(47.734145, Latitude.DEGREES))));
    }

    @Test
    public void testDoesNotContain() {
        Assert.assertTrue(!SECTOR.contains(new Geodetic2DPoint(
                new Longitude(122.2873886, Latitude.DEGREES),
                new Latitude(47.4689923, Longitude.DEGREES))));
    }

    @Test
    public void testCanonicalPointIsCenter() {
        Assert.assertEquals(CENTER, SECTOR.getCanonicalPoint());
    }

    @Test
    public void testNameIsBounds() {
        Assert.assertEquals(
                "(122° 27' 35\" W, 47° 28' 54\" N) .. (122° 13' 28\" W, 47° 44' 3\" N)",
                SECTOR.getCommonName());
    }

    @Test
    public void testIdIsBounds() {
        Assert.assertEquals(
                "(122° 27' 35\" W, 47° 28' 54\" N) .. (122° 13' 28\" W, 47° 44' 3\" N)",
                SECTOR.getIdentifier());
    }

    @Test
    public void testInteriorPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.2986405, Latitude.DEGREES),
                new Latitude(47.6081435, Longitude.DEGREES));

        Assert.assertEquals(point, SECTOR.getNearestPoint(point));
    }

    @Test
    public void testNorthPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.3471709, Longitude.DEGREES),
                new Latitude(47.7390928, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.3471709, Longitude.DEGREES),
                new Latitude(47.734145, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }

    @Test
    public void testNortheastPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-121.9724687, Longitude.DEGREES),
                new Latitude(47.858788, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.224433, Longitude.DEGREES),
                new Latitude(47.734145, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }

    @Test
    public void testEastPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.1451506, Longitude.DEGREES),
                new Latitude(47.6306827, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.224433, Longitude.DEGREES),
                new Latitude(47.6306827, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }

    @Test
    public void testSoutheastPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.2093739, Longitude.DEGREES),
                new Latitude(47.4794872, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.224433, Longitude.DEGREES),
                new Latitude(47.48172, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }

    @Test
    public void testSouthPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.2992092, Longitude.DEGREES),
                new Latitude(47.4229401, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.2992092, Longitude.DEGREES),
                new Latitude(47.48172, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }

    @Test
    public void testSouthwestPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.9007201, Longitude.DEGREES),
                new Latitude(47.0431533, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.459696, Longitude.DEGREES),
                new Latitude(47.48172, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));

    }

    @Test
    public void testWestPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-122.5227446, Longitude.DEGREES),
                new Latitude(47.6246413, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.459696, Longitude.DEGREES),
                new Latitude(47.6246413, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }

    @Test
    public void testNorthwestPoint() {
        final Geodetic2DPoint point = new Geodetic2DPoint(
                new Longitude(-123.0167792, Longitude.DEGREES),
                new Latitude(48.5347813, Latitude.DEGREES));
        Assert.assertEquals(new Geodetic2DPoint(
                new Longitude(-122.459696, Longitude.DEGREES),
                new Latitude(47.734145, Latitude.DEGREES)),
                            SECTOR.getNearestPoint(point));
    }
    
    @Test
    public void testVisited() throws Exception {
        final CountingVisitor visitor = new CountingVisitor();
        SECTOR.accept(visitor);
        Assert.assertEquals(1, visitor.getSectorCount());
    }

}
