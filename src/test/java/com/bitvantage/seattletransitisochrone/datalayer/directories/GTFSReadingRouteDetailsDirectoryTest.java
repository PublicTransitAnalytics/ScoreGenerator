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
package com.bitvantage.seattletransitisochrone.datalayer.directories;

import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingRouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.RouteIdKey;
import com.google.common.collect.ImmutableMap;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingRouteDetailsDirectoryTest {

    @Test
    public void testPutsRecords() throws Exception {
        final MapStore<RouteIdKey, RouteDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color\n"
                + "100001,KCM,\"1\",\"\",\"Kinnear - Downtown Seattle\",3,http://metro.kingcounty.gov/schedules/001/n0.html,,\n"
                + "100002,KCM,\"10\",\"\",\"Capitol Hill - Downtown Seattle\",3,http://metro.kingcounty.gov/schedules/010/n0.html,,");

        final RouteDetailsDirectory directory
                = new GTFSReadingRouteDetailsDirectory(store, reader);
        Assert.assertEquals(2, store.getValues().size());
    }

    @Test
    public void testCreatesValue() throws Exception {
        final MapStore<RouteIdKey, RouteDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color\n"
                + "100001,KCM,\"1\",\"\",\"Kinnear - Downtown Seattle\",3,http://metro.kingcounty.gov/schedules/001/n0.html,,\n");
        final RouteDetailsDirectory directory
                = new GTFSReadingRouteDetailsDirectory(store, reader);
        Assert.assertEquals(new RouteDetails("1", "Kinnear - Downtown Seattle"),
                            store.getValues().iterator().next());
    }

    @Test
    public void testCreatesKey() throws Exception {
        final MapStore<RouteIdKey, RouteDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color\n"
                + "100001,KCM,\"1\",\"\",\"Kinnear - Downtown Seattle\",3,http://metro.kingcounty.gov/schedules/001/n0.html,,\n");
        final RouteDetailsDirectory directory
                = new GTFSReadingRouteDetailsDirectory(store, reader);
        Assert.assertTrue(store.containsKey(new RouteIdKey("100001")));
    }

    @Test
    public void testGet() throws Exception {
        final MapStore<RouteIdKey, RouteDetails> store
                = new MapStore<>(new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color\n"
                + "100001,KCM,\"1\",\"\",\"Kinnear - Downtown Seattle\",3,http://metro.kingcounty.gov/schedules/001/n0.html,,\n");
        final RouteDetailsDirectory directory
                = new GTFSReadingRouteDetailsDirectory(store, reader);
        Assert.assertEquals(new RouteDetails("1", "Kinnear - Downtown Seattle"),
                            directory.getRouteDetails("100001"));
    }

    @Test
    public void testDoesNotRebuild() throws Exception {
        final ImmutableMap<String, RouteDetails> immutableMap
                = ImmutableMap.of(
                        new RouteIdKey("100001").toString(),
                        new RouteDetails("1", "Kinnear - Downtown Seattle"));
        final MapStore<RouteIdKey, RouteDetails> store
                = new MapStore<>(immutableMap);
        final Reader reader = new StringReader(
                "route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color\n"
                + "100001,KCM,\"1\",\"\",\"Kinnear - Downtown Seattle\",3,http://metro.kingcounty.gov/schedules/001/n0.html,,\n");
        final RouteDetailsDirectory directory
                = new GTFSReadingRouteDetailsDirectory(store, reader);
    }

}
