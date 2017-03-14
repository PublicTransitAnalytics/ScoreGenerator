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

import com.publictransitanalytics.scoregenerator.tracking.TransitRideMovement;
import java.time.LocalDateTime;
import java.time.Month;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class TransitRideMovementTest {

    @Test
    public void testShortName() {
        final TransitRideMovement movement = new TransitRideMovement(
                "tripId", "1", "Somewhere via Esewhere", "10060", 
                "NE 55th St & 27th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                "10070", "NE 55th St & 25th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals("1", movement.getShortForm());
    }

    @Test
    public void testWalkingDistance() {
        final TransitRideMovement movement = new TransitRideMovement(
                "tripId", "1", "Somewhere via Esewhere", "10060", 
                "NE 55th St & 27th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                "10070", "NE 55th St & 25th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(0.0, movement.getWalkingDistance());
    }

    @Test
    public void testStartTime() {
        final TransitRideMovement movement = new TransitRideMovement(
                "tripId", "1", "Somewhere via Esewhere", "10060", 
                "NE 55th St & 27th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                "10070", "NE 55th St & 25th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                movement.getStartTime());
    }
    
    @Test
    public void testEndTime() {
        final TransitRideMovement movement = new TransitRideMovement(
                "tripId", "1", "Somewhere via Esewhere", "10060", 
                "NE 55th St & 27th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 30, 0),
                "10070", "NE 55th St & 25th Ave NE",
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0));
        Assert.assertEquals(
                LocalDateTime.of(2017, Month.JANUARY, 29, 16, 31, 0),
                movement.getEndTime());
    }

}
