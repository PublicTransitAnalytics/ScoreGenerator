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
package com.bitvantage.seattletransitisochrone.datalayer.directories.types;

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import java.time.Duration;
import java.time.LocalTime;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class TransitTimeTest {

    @Test
    public void testConvertLocalTime() {
        final LocalTime time = LocalTime.of(10, 10, 10);

        Assert.assertEquals(new TransitTime((byte) 10, (byte) 10, (byte) 10),
                            TransitTime.fromLocalTime(time));
    }

    @Test
    public void testConvertLocalTimeWithOverflow() {
        final LocalTime time = LocalTime.of(10, 10, 10);

        Assert.assertEquals(new TransitTime((byte) 34, (byte) 10, (byte) 10),
                            TransitTime.fromLocalTimeWithOverflow(time));
    }

    @Test
    public void testInvalidSeconds() {
        try {
            new TransitTime((byte) 10, (byte) 10, (byte) 70);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testNegativeSeconds() {
        try {
            new TransitTime((byte) 10, (byte) 10, (byte) -10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testInvalidMinutes() {
        try {
            new TransitTime((byte) 10, (byte) 70, (byte) 10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testNegativeMinutes() {
        try {
            new TransitTime((byte) 10, (byte) -10, (byte) 10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testInvalidHours() {
        try {
            new TransitTime((byte) 49, (byte) 10, (byte) 10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testNegitaveHours() {
        try {
            new TransitTime((byte) -10, (byte) 10, (byte) 10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testParse() {
        Assert.assertEquals(new TransitTime((byte) 10, (byte) 10, (byte) 10),
                            TransitTime.parse("10:10:10"));
    }

    @Test
    public void testParseIvalid() {
        try {
            TransitTime.parse("10:10/10");
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testDuration() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 11, (byte) 11, (byte) 11);

        Assert.assertEquals(Duration.ofSeconds(3661), TransitTime.durationBetween(first, second));
    }

    @Test
    public void testComparisonBeforeHours() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 11, (byte) 11, (byte) 11);

        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(first.isBefore(second));
        Assert.assertFalse(first.isAfter(second));
    }

    @Test
    public void testComparisonBeforeMinutes() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 10, (byte) 11, (byte) 11);

        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(first.isBefore(second));
        Assert.assertFalse(first.isAfter(second));
    }

    @Test
    public void testComparisonBeforeSeconds() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 10, (byte) 10, (byte) 11);

        Assert.assertTrue(first.compareTo(second) < 0);
        Assert.assertTrue(first.isBefore(second));
        Assert.assertFalse(first.isAfter(second));
    }

    @Test
    public void testComparisonAfterHour() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 11, (byte) 11, (byte) 11);

        Assert.assertTrue(second.compareTo(first) > 0);
        Assert.assertTrue(second.isAfter(first));
        Assert.assertFalse(second.isBefore(first));
    }

    @Test
    public void testComparisonAfterMinutes() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 10, (byte) 11, (byte) 11);

        Assert.assertTrue(second.compareTo(first) > 0);
        Assert.assertTrue(second.isAfter(first));
        Assert.assertFalse(second.isBefore(first));
    }

    @Test
    public void testComparisonAfterSeconds() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 10, (byte) 10, (byte) 11);

        Assert.assertTrue(second.compareTo(first) > 0);
        Assert.assertTrue(second.isAfter(first));
        Assert.assertFalse(second.isBefore(first));
    }

    @Test
    public void testComparisonSameTime() {
        final TransitTime first = new TransitTime((byte) 10, (byte) 10, (byte) 10);
        final TransitTime second = new TransitTime((byte) 10, (byte) 10, (byte) 10);

        Assert.assertTrue(second.compareTo(first) == 0);
        Assert.assertTrue(first.compareTo(second) == 0);
    }

    @Test
    public void testSecondAdd() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofSeconds(12));
        Assert.assertEquals(new TransitTime((byte) 3, (byte) 45, (byte) 45), added);
    }

    @Test
    public void testSecondAddOverflow() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofSeconds(27));
        Assert.assertEquals(new TransitTime((byte) 3, (byte) 46, (byte) 00), added);
    }

    @Test
    public void testSecondAddOverflowAndRemainder() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofSeconds(33));
        Assert.assertEquals(new TransitTime((byte) 3, (byte) 46, (byte) 6), added);
    }

    @Test
    public void testSecondAddDoubleOverflowAndRemainder() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofSeconds(93));
        Assert.assertEquals(new TransitTime((byte) 3, (byte) 47, (byte) 6), added);
    }

    @Test
    public void testMinuteAdd() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofMinutes(10));
        Assert.assertEquals(new TransitTime((byte) 3, (byte) 55, (byte) 33), added);
    }

    @Test
    public void testMinuteAddOverflow() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofMinutes(15));
        Assert.assertEquals(new TransitTime((byte) 4, (byte) 00, (byte) 33), added);
    }

    @Test
    public void testMinuteAddOverflowAndRemainder() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofMinutes(16));
        Assert.assertEquals(new TransitTime((byte) 4, (byte) 1, (byte) 33), added);
    }

    @Test
    public void testMinuteAddDoubleOverflowAndRemainder() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofMinutes(76));
        Assert.assertEquals(new TransitTime((byte) 5, (byte) 1, (byte) 33), added);
    }

    @Test
    public void testHourAdd() {
        TransitTime base = new TransitTime((byte) 3, (byte) 45, (byte) 33);
        TransitTime added = base.plus(Duration.ofHours(22));
        Assert.assertEquals(new TransitTime((byte) 25, (byte) 45, (byte) 33), added);
    }

    @Test
    public void testSubtract() {
        TransitTime base = new TransitTime((byte) 10, (byte) 45, (byte) 33);
        TransitTime subtracted = base.minus(Duration.ofSeconds(3661));

        Assert.assertEquals(new TransitTime((byte) 9, (byte) 44, (byte) 32), subtracted);
    }

    @Test
    public void testSubtractInvalid() {
        TransitTime base = new TransitTime((byte) 10, (byte) 45, (byte) 33);

        try {
            base.minus(Duration.ofHours(11));
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
    }
}
