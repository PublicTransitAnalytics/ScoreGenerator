/*
 * Copyright 2018 Public Transit Analytics.
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
import com.publictransitanalytics.scoregenerator.environment.Segment;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GridPointTest {

    final GeoLatitude LATITUDE
            = new GeoLatitude("47.66", AngleUnit.DEGREES);
    final Segment SEGMENT = new Segment(
            new GeoPoint(
                    new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                    new GeoLatitude("47.661389", AngleUnit.DEGREES)),
            new GeoPoint(
                    new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                    new GeoLatitude("47.661389", AngleUnit.DEGREES)), 0, 0);

    @Test
    public void testEquality() {
        final GeoPoint loc1 = new GeoPoint(
                new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                new GeoLatitude("47.661389", AngleUnit.DEGREES));
        final GeoPoint loc2 = new GeoPoint(
                new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                new GeoLatitude("47.661389", AngleUnit.DEGREES));

        final GridPoint point1 = new GridPoint(loc1, SEGMENT, LATITUDE);
        final GridPoint point2 = new GridPoint(loc2, SEGMENT, LATITUDE);
        Assert.assertEquals(point1, point2);
    }

    @Test
    public void testHashCode() {
        final GeoPoint loc1 = new GeoPoint(
                new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                new GeoLatitude("47.661389", AngleUnit.DEGREES));
        final GeoPoint loc2 = new GeoPoint(
                new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                new GeoLatitude("47.661389", AngleUnit.DEGREES));
        final GridPoint point1 = new GridPoint(loc1, SEGMENT, LATITUDE);
        final GridPoint point2 = new GridPoint(loc2, SEGMENT, LATITUDE);
        Assert.assertEquals(point1.hashCode(), point2.hashCode());
    }

}
