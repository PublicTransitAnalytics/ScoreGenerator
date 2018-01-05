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
package com.publictransitanalytics.scoregenerator.datalayer.directories;

import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.Coordinate;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.StopDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopIdKey;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingStopDetailsDirectoryTest {

    @Test
    public void testPutsRecords() throws Exception {
        final MapStore<StopIdKey, StopDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone\n"
                + "10000,,\"NE 55th St & 43rd Ave NE\",,47.6685753,-122.283653,21,,0,,America/Los_Angeles\n"
                + "10005,,\"40th Ave NE & NE 51st St\",,47.6658859,-122.284897,21,,0,,America/Los_Angeles");

        final GTFSReadingStopDetailsDirectory directory
                = new GTFSReadingStopDetailsDirectory(store, reader);
        Assert.assertEquals(2, store.getValues().size());
    }

    @Test
    public void testCreatesValue() throws Exception {
        final MapStore<StopIdKey, StopDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone\n"
                + "10000,,\"NE 55th St & 43rd Ave NE\",,47.6685753,-122.283653,21,,0,,America/Los_Angeles\n");

        final GTFSReadingStopDetailsDirectory directory
                = new GTFSReadingStopDetailsDirectory(store, reader);
        Assert.assertEquals(new StopDetails(
                "10000", "NE 55th St & 43rd Ave NE",
                new Coordinate("47.6685753", "-122.283653")),
                            store.getValues().iterator().next());
    }

    @Test
    public void testCreatesKey() throws Exception {
        final MapStore<StopIdKey, StopDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone\n"
                + "10000,,\"NE 55th St & 43rd Ave NE\",,47.6685753,-122.283653,21,,0,,America/Los_Angeles\n");

        final GTFSReadingStopDetailsDirectory directory
                = new GTFSReadingStopDetailsDirectory(store, reader);
        Assert.assertTrue(store.containsKey(new StopIdKey("10000")));
    }

    @Test
    public void testGet() throws Exception {
        final MapStore<StopIdKey, StopDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone\n"
                + "10000,,\"NE 55th St & 43rd Ave NE\",,47.6685753,-122.283653,21,,0,,America/Los_Angeles\n");

        final GTFSReadingStopDetailsDirectory directory
                = new GTFSReadingStopDetailsDirectory(store, reader);
        Assert.assertEquals(new StopDetails(
                "10000", "NE 55th St & 43rd Ave NE",
                new Coordinate("47.6685753", "-122.283653")),
                            directory.getDetails("10000"));
    }

    @Test
    public void testGetsAll() throws Exception {
        final MapStore<StopIdKey, StopDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone\n"
                + "10000,,\"NE 55th St & 43rd Ave NE\",,47.6685753,-122.283653,21,,0,,America/Los_Angeles\n"
                + "10005,,\"40th Ave NE & NE 51st St\",,47.6658859,-122.284897,21,,0,,America/Los_Angeles");

        final GTFSReadingStopDetailsDirectory directory
                = new GTFSReadingStopDetailsDirectory(store, reader);
        Assert.assertEquals(ImmutableMultiset.of(
                new StopDetails("10000", "NE 55th St & 43rd Ave NE",
                                new Coordinate("47.6685753", "-122.283653")),
                new StopDetails("10005", "40th Ave NE & NE 51st St",
                                new Coordinate("47.6658859", "-122.284897"))),
                            directory.getAllStopDetails());
    }

    @Test
    public void testDoesNotRebuild() throws Exception {
        final ImmutableMap<String, StopDetails> immutableMap
                = ImmutableMap.of(
                        new StopIdKey("10000").getKeyString(), new StopDetails(
                                "10000", "NE 55th St & 43rd Ave NE",
                                new Coordinate("47.6685753", "-122.283653")));

        final MapStore<StopIdKey, StopDetails> store
                = new MapStore<>(immutableMap);
        final Reader reader = new StringReader(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone\n"
                + "10000,,\"NE 55th St & 43rd Ave NE\",,47.6685753,-122.283653,21,,0,,America/Los_Angeles\n");

        final GTFSReadingStopDetailsDirectory directory
                = new GTFSReadingStopDetailsDirectory(store, reader);
    }

}
