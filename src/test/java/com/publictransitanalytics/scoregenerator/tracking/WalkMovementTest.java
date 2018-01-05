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

import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class WalkMovementTest {

    private static final Landmark POINT = new Landmark(
            new GeoPoint(
                    new GeoLongitude("-122.31952",
                                            AngleUnit.DEGREES),
                    new GeoLatitude("47.545946",
                                           AngleUnit.DEGREES)));

    @Test
    public void testShortName() throws Exception {
        final WalkMovement movement = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 29, 15, 45, 0), 111.1,
                POINT, LocalDateTime.of(2017, Month.JANUARY, 29, 15, 55, 0),
                POINT);

        Assert.assertEquals("Walk", movement.getShortForm());
    }

}
