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

import com.publictransitanalytics.scoregenerator.tracking.WalkMovement;
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
public class WalkMovementTest {

    @Test
    public void testShortName() {
        final WalkMovement movement = new WalkMovement(
                LocalDateTime.of(2017, Month.JANUARY, 29, 15, 45, 0),
                111.1, new Geodetic2DPoint(
                        new Longitude(-122.319523, Longitude.DEGREES),
                        new Latitude(47.5459458, Latitude.DEGREES)),
                LocalDateTime.of(2017, Month.JANUARY, 29, 15, 55, 0),
                new Geodetic2DPoint(
                        new Longitude(-122.3194539, Longitude.DEGREES),
                        new Latitude(47.5502334, Latitude.DEGREES)));
                
        Assert.assertEquals("Walk", movement.getShortForm());
    }

}
