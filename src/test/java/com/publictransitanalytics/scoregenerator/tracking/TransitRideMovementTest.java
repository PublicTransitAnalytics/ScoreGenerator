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

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class TransitRideMovementTest {

    private static final String TRIP_ID = "tripId";
    private static final String ROUTE_NUMBER = "1";
    private static final Trip TRIP = new Trip(
            new TripId(TRIP_ID, null), "Somewhere via Elsewhere", ROUTE_NUMBER, 
            Collections.emptySet());
    private static final TransitStop BEGINNING_STOP 
            = new TransitStop("0", "Origin", null);
        private static final TransitStop END_STOP 
            = new TransitStop("1", "Elsewhere", null);

    @Test
    public void testShortName() throws Exception {
        final TransitRideMovement movement = new TransitRideMovement(
                TRIP, BEGINNING_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                END_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(ROUTE_NUMBER, movement.getShortForm());
    }

    @Test
    public void testWalkingDistance() throws Exception {
        final TransitRideMovement movement = new TransitRideMovement(
                TRIP, BEGINNING_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                END_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(0.0, movement.getWalkingDistance());
    }

    @Test
    public void testStartTime() throws Exception {
        final TransitRideMovement movement = new TransitRideMovement(
                TRIP, BEGINNING_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                END_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                movement.getStartTime());
    }

    @Test
    public void testEndTime() throws Exception {
        final TransitRideMovement movement = new TransitRideMovement(
                TRIP, BEGINNING_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                END_STOP,
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0),
                movement.getEndTime());
    }

}
