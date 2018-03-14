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

import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.google.common.collect.ImmutableMap;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingTripDetailsDirectoryTest {

    @Test
    public void testPutsTrips() throws Exception {
        final Store<TripGroupKey, TripDetails> store = new MapStore<>(
                new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,peak_flag,fare_id\n"
                + "100340,4425,11367651,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984607,20098504,0,101\n"
                + "100340,4425,11367652,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984608,20098504,0,101");
        new GTFSReadingTripDetailsDirectory(store, reader);
        Assert.assertEquals(2, store.getValues().size());
    }

    @Test
    public void testMakesValue() throws Exception {
        final Store<TripGroupKey, TripDetails> store = new MapStore<>(
                new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,peak_flag,fare_id\n"
                + "100340,4425,11367651,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984607,20098504,0,101");
        new GTFSReadingTripDetailsDirectory(store, reader);
        Assert.assertEquals(new TripDetails("11367651", "100340", "4425"),
                            store.getValues().iterator().next());
    }

    @Test
    public void testMakesKey() throws Exception {
        final Store<TripGroupKey, TripDetails> store = new MapStore<>(
                new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,peak_flag,fare_id\n"
                + "100340,4425,11367651,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984607,20098504,0,101");
        new GTFSReadingTripDetailsDirectory(store, reader);
        Assert.assertTrue(store.containsKey(new TripGroupKey("11367651")));
    }

    @Test
    public void testGets() throws Exception {
        final Store<TripGroupKey, TripDetails> store = new MapStore<>(
                new HashMap<>());
        final Reader reader = new StringReader(
                "route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,peak_flag,fare_id\n"
                + "100340,4425,11367651,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984607,20098504,0,101");
        final GTFSReadingTripDetailsDirectory directory
                = new GTFSReadingTripDetailsDirectory(store, reader);
        Assert.assertEquals(
                new TripDetails("11367651", "100340", "4425"),
                directory.getTripDetails(new TripGroupKey("11367651")));
    }

    @Test
    public void testDoesNotRebuild() throws Exception {
        final ImmutableMap<String, TripDetails> immutableMap = ImmutableMap.of(
                new TripGroupKey("11367651").getKeyString(),
                new TripDetails("11367651", "100340", "4425"));
        final Store<TripGroupKey, TripDetails> store
                = new MapStore<>(immutableMap);
        final Reader reader = new StringReader(
                "route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,peak_flag,fare_id\n"
                + "100340,4425,11367651,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984607,20098504,0,101\n"
                + "100340,4425,11367652,\"DOWNTOWN SEATTLE PACIFIC PLACE STATION\",\"LOCAL\",1,3984608,20098504,0,101");
        new GTFSReadingTripDetailsDirectory(store, reader);
    }

}
