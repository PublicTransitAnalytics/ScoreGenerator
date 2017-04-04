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
package com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys;

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopTimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class StopTimeKeyTest {

    private static final String TEST_STOP_ID = "stopId";

    @Test
    public void testWriteKey() {
        final StopTimeKey key = StopTimeKey.getWriteKey(
                TEST_STOP_ID, new TransitTime((byte) 11, (byte) 11, (byte) 11));
        Assert.assertTrue(key.getKeyString().startsWith("stopId::11:11:11::"));
    }

    @Test
    public void testMinKey() {
        final StopTimeKey key = StopTimeKey.getMinKey(
                TEST_STOP_ID, new TransitTime((byte) 11, (byte) 11, (byte) 11));
        Assert.assertEquals(
                "stopId::11:11:11::00000000-0000-0000-0000-000000000000",
                key.getKeyString());
    }

    @Test
    public void testMaxKey() {
        final StopTimeKey key = StopTimeKey.getMaxKey(
                TEST_STOP_ID, new TransitTime((byte) 11, (byte) 11, (byte) 11));
        Assert.assertEquals(
                "stopId::11:11:11::ffffffff-ffff-ffff-ffff-ffffffffffff",
                key.getKeyString());
    }

    @Test
    public void testPadsKey() {
        final StopTimeKey key = StopTimeKey.getMaxKey(
                TEST_STOP_ID, new TransitTime((byte) 9, (byte) 9, (byte) 9));
        Assert.assertEquals(
                "stopId::09:09:09::ffffffff-ffff-ffff-ffff-ffffffffffff",
                key.getKeyString());
    }

    @Test
    public void testRangeMax() {
        final StopTimeKey key = StopTimeKey.getMaxKey(
                TEST_STOP_ID, new TransitTime((byte) 23, (byte) 00, (byte) 00));
        Assert.assertEquals(
                "stopId::47:59:59::ffffffff-ffff-ffff-ffff-ffffffffffff",
                key.getRangeMax().getKeyString());
    }

    @Test
    public void testRangeMin() {
        final StopTimeKey key = StopTimeKey.getMaxKey(
                TEST_STOP_ID, new TransitTime((byte) 23, (byte) 00, (byte) 00));
        Assert.assertEquals(
                "stopId::00:00:00::00000000-0000-0000-0000-000000000000",
                key.getRangeMin().getKeyString());
    }
}
