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
package com.publictransitanalytics.scoregenerator;

import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.location.Sector;
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
public class SectorTableTest {

    private static final SectorTable TABLE = new SectorTable(
            new Geodetic2DBounds(
                    new Geodetic2DPoint(
                            new Longitude(-130, Longitude.DEGREES),
                            new Latitude(60, Latitude.DEGREES)),
                    new Geodetic2DPoint(
                            new Longitude(-100, Longitude.DEGREES),
                            new Latitude(40, Latitude.DEGREES))), 2, 3);

    @Test
    public void testGeneratesSectors() {
        Assert.assertEquals(6, TABLE.getSectors().size());
    }

    @Test
    public void testReturnsCornerSector() {

        final Sector sector = TABLE.findSector(
                new Geodetic2DPoint(
                        new Longitude(-100, Longitude.DEGREES),
                        new Latitude(40, Latitude.DEGREES)));

        Assert.assertEquals(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-110, Longitude.DEGREES),
                        new Latitude(50, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-100, Longitude.DEGREES),
                        new Latitude(40, Latitude.DEGREES))),
                            sector.getBounds());
    }

    @Test
    public void testReturnsInteriorSector() {

        final Sector sector = TABLE.findSector(
                new Geodetic2DPoint(
                        new Longitude(-111, Longitude.DEGREES),
                        new Latitude(41, Latitude.DEGREES)));

        Assert.assertEquals(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-120, Longitude.DEGREES),
                        new Latitude(50, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-110, Longitude.DEGREES),
                        new Latitude(40, Latitude.DEGREES))),
                            sector.getBounds());
    }

    @Test
    public void testReturnsGreatestSector() {

        final Sector sector = TABLE.findSector(
                new Geodetic2DPoint(
                        new Longitude(-129.95, Longitude.DEGREES),
                        new Latitude(59, Latitude.DEGREES)));

        Assert.assertEquals(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-130, Longitude.DEGREES),
                        new Latitude(60, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-120, Longitude.DEGREES),
                        new Latitude(50, Latitude.DEGREES))),
                            sector.getBounds());
    }

    @Test
    public void testNullOnOutofBoundsLatitude() {

        final Sector sector = TABLE.findSector(
                new Geodetic2DPoint(
                        new Longitude(-111, Longitude.DEGREES),
                        new Latitude(35, Latitude.DEGREES)));

        Assert.assertNull(sector);
    }

    @Test
    public void testNullOnOutofBoundsLongitude() {

        final Sector sector = TABLE.findSector(
                new Geodetic2DPoint(
                        new Longitude(-135, Longitude.DEGREES),
                        new Latitude(45, Latitude.DEGREES)));

        Assert.assertNull(sector);
    }

}
