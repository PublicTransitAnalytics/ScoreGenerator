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
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import com.publictransitanalytics.scoregenerator.testhelpers.CountingVisitor;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class TransitStopTest {

    public final static String STOP_ID = "stopId";
    private final static String STOP_NAME = "stopName";

    private final static GeoPoint POINT = new GeoPoint(
            new GeoLongitude("-122.33618", AngleUnit.DEGREES),
            new GeoLatitude("47.620691", AngleUnit.DEGREES));

    @Test
    public void testStopIdIsIdentifier() {
        final TransitStop stop = new TransitStop(STOP_ID, STOP_NAME, POINT);
        Assert.assertEquals(STOP_ID, stop.getIdentifier());
    }

    @Test
    public void testStopNameIsCommonName() {
        final TransitStop stop = new TransitStop(STOP_ID, STOP_NAME, POINT);
        Assert.assertEquals(STOP_NAME, stop.getCommonName());
    }

    @Test
    public void testVisited() throws Exception {
        final TransitStop stop = new TransitStop(STOP_ID, STOP_NAME, POINT);
        final CountingVisitor visitor = new CountingVisitor();
        stop.accept(visitor);
        Assert.assertEquals(1, visitor.getTransitStopCount());
    }
}
