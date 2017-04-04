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
package com.publictransitanalytics.scoregenerator.walking;

import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class ForwardTimeTrackerTest {

    @Test
    public void testAdjusts() {
        final ForwardTimeTracker adjuster = new ForwardTimeTracker();
        final LocalDateTime adjusted = adjuster.adjust(
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 0, 0),
                Duration.ofMinutes(10));

        Assert.assertEquals(LocalDateTime.of(2017, Month.FEBRUARY, 1,
                                             10, 10, 0), adjusted);
    }

    @Test
    public void testCanAdjustToCutoff() {
        final ForwardTimeTracker adjuster = new ForwardTimeTracker();
        Assert.assertTrue(adjuster.canAdjust(
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 0, 0),
                Duration.ofMinutes(10),
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 10, 0)));
    }

    @Test
    public void testCanAdjustShortOfCutoff() {
        final ForwardTimeTracker adjuster = new ForwardTimeTracker();
        Assert.assertTrue(adjuster.canAdjust(
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 0, 0),
                Duration.ofMinutes(9),
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 10, 0)));
    }

    @Test
    public void testCannotAdjustBeyondCutoff() {
        final ForwardTimeTracker adjuster = new ForwardTimeTracker();
        Assert.assertTrue(!adjuster.canAdjust(
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 0, 0),
                Duration.ofMinutes(11),
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 10, 0)));
    }
        
    @Test
    public void testGetDuration() {
        final ForwardTimeTracker adjuster = new ForwardTimeTracker();
        final Duration duration = adjuster.getDuration(
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 0, 0),
                LocalDateTime.of(2017, Month.FEBRUARY, 1, 10, 10, 0));
        Assert.assertEquals(Duration.ofMinutes(10), duration);
    }

}
