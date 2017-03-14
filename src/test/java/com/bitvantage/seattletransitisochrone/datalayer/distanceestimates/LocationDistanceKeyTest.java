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
package com.bitvantage.seattletransitisochrone.datalayer.distanceestimates;

import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationDistanceKey;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class LocationDistanceKeyTest {

    @Test
    public void testMin() {
        final LocationDistanceKey key = LocationDistanceKey.getMinKey("id",
                                                                      11111111.111111);
        Assert.assertEquals(
                "id::11111111.111111::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testMax() {
        final LocationDistanceKey key = LocationDistanceKey.getMaxKey("id",
                                                                      11111111.111111);
        Assert.assertEquals(
                "id::11111111.111111::ffffffff-ffff-ffff-ffff-ffffffffffff",
                key.getKeyString());
    }

    @Test
    public void testPads() {
        final LocationDistanceKey key = LocationDistanceKey.getMinKey("id", 1.1);
        Assert.assertEquals(
                "id::00000001.100000::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testTruncates() {
        final LocationDistanceKey key = LocationDistanceKey.getMinKey("id",
                                                                      1.1111111);
        Assert.assertEquals(
                "id::00000001.111111::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testHighest() {
        final LocationDistanceKey key = LocationDistanceKey.getMinKey("id",
                                                                      20000000);
        Assert.assertEquals(
                "id::20000000.000000::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testRangeMax() {
        final LocationDistanceKey key
                = LocationDistanceKey.getWriteKey("id", 101.101).getRangeMax();
        Assert.assertEquals(
                "id::20000000.000000::ffffffff-ffff-ffff-ffff-ffffffffffff",
                key.getKeyString());
    }

    @Test
    public void testRangeMin() {
        final LocationDistanceKey key
                = LocationDistanceKey.getWriteKey("id", 101.101).getRangeMin();
        Assert.assertEquals(
                "id::00000000.000000::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testTooHigh() {
        try {
            final LocationDistanceKey key
                    = LocationDistanceKey.getMinKey("id", 20000000.1);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testLowest() {
        final LocationDistanceKey key = LocationDistanceKey.getMinKey("id", 0);
        Assert.assertEquals(
                "id::00000000.000000::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testTooLow() {
        try {
            final LocationDistanceKey key = LocationDistanceKey.getMinKey("id",
                                                                          -0.000001);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
    }
}
