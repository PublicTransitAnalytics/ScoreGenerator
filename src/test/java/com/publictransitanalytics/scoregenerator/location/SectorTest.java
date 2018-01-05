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

import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoBounds;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class SectorTest {

    private final static Sector SECTOR = new Sector(new GeoBounds(
            new GeoLongitude("-122.45970", AngleUnit.DEGREES),
            new GeoLatitude("47.48172", AngleUnit.DEGREES),
            new GeoLongitude("-122.22443", AngleUnit.DEGREES),
            new GeoLatitude("47.734145", AngleUnit.DEGREES)));
    final static GeoPoint CENTER = new GeoPoint(
            new GeoLongitude("-2.1352719", AngleUnit.RADIANS),
            new GeoLatitude("0.83091518", AngleUnit.RADIANS));

    @Test
    public void testContains() {
        Assert.assertTrue(SECTOR.contains(new GeoPoint(
                new GeoLongitude("-122.2986405", AngleUnit.DEGREES),
                new GeoLatitude("47.6081435", AngleUnit.DEGREES))));
    }

    @Test
    public void testContainsCorner() {
        Assert.assertTrue(SECTOR.contains(new GeoPoint(
                new GeoLongitude("-122.459696", AngleUnit.DEGREES),
                new GeoLatitude("47.734145", AngleUnit.DEGREES))));
    }

    @Test
    public void testDoesNotContain() {
        Assert.assertTrue(!SECTOR.contains(new GeoPoint(
                new GeoLongitude("122.2873886", AngleUnit.DEGREES),
                new GeoLatitude("47.4689923", AngleUnit.DEGREES))));
    }

    @Test
    public void testCanonicalPointIsCenter() {
        Assert.assertEquals(CENTER, SECTOR.getCenter());
    }

    @Test
    public void testNameIsDegreeBounds() {
        Assert.assertEquals("-122.459700, 47.481720, -122.224430, 47.734145",
                            SECTOR.getCommonName());
    }

    @Test
    public void testIdIsRadianBounds() {
        Assert.assertEquals("-2.137325, 0.828712, -2.133219, 0.833118",
                            SECTOR.getIdentifier());
    }

}
