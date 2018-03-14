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

import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.testhelpers.CountingVisitor;
import org.junit.Assert;
import org.junit.Test;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;

/**
 *
 * @author Public Transit Analytics
 */
public class LandmarkTest {

    private static final GeoPoint POINT = new GeoPoint(
            new GeoLongitude("-122.32370", AngleUnit.DEGREES),
            new GeoLatitude("47.654656", AngleUnit.DEGREES));

    @Test
    public void testVisit() throws Exception {
        final Landmark landmark = new Landmark(POINT);
        final CountingVisitor visitor = new CountingVisitor();
        landmark.accept(visitor);
        Assert.assertEquals(1, visitor.getLandmarkCount());
    }

    @Test
    public void testIdentifierIsRadianLocation() {
        final Landmark landmark = new Landmark(POINT);
        Assert.assertEquals("0.831731, -2.134951", landmark.getIdentifier());
    }

    @Test
    public void testNameIsDegreeLocation() {
        final Landmark landmark = new Landmark(POINT);
        Assert.assertEquals("47.654656, -122.323700", landmark.getCommonName());
    }

}
