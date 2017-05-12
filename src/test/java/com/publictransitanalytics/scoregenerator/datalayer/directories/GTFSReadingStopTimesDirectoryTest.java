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

import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.MapRangedStore;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopTimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripSequenceKey;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingStopTimesDirectoryTest {

    @Test
    public void testGetsAllStopsAtStopWithoutFrequencies() throws Exception {
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = new MapRangedStore<TripSequenceKey, TripStop>(
                        new TreeMap<>());
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = new MapRangedStore<StopTimeKey, TripStop>(new TreeMap<>());
        final Store<TripIdKey, TripId> tripsStore
                = new MapStore<>(new HashMap<>());

        final Reader frequenciesReader = new StringReader("");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n"
                + "11367651,05:45:05,05:45:05,26700,4,\"\",0,0,549.2,1\n"
                        + "11367651,05:47:06,05:47:06,26702,13,\"\",0,0,1879.6,1\n"
                + "11367652,06:02:06,06:02:06,26702,13,\"\",0,0,1879.6,1");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                tripSequenceStore, stopTimesStore, tripsStore,
                frequenciesReader, stopTimesReader);
        final List<TripStop> stops = directory.getStopsAtStopInRange(
                "26702", new TransitTime((byte) 5, (byte) 47, (byte) 0),
                new TransitTime((byte) 6, (byte) 3, (byte) 0));
        Assert.assertEquals(2, stops.size());
    }

    @Test
    public void testGetsAllStopsOnTripWithoutFrequencies() throws Exception {
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = new MapRangedStore<TripSequenceKey, TripStop>(
                        new TreeMap<>());
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = new MapRangedStore<StopTimeKey, TripStop>(new TreeMap<>());
        final Store<TripIdKey, TripId> tripsStore
                = new MapStore<>(new HashMap<>());

        final Reader frequenciesReader = new StringReader("");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n"
                + "11367651,05:45:05,05:45:05,26700,4,\"\",0,0,549.2,1\n"
                        + "11367651,05:47:06,05:47:06,26702,13,\"\",0,0,1879.6,1\n"
                + "11367652,06:02:06,06:02:06,26702,13,\"\",0,0,1879.6,1");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                tripSequenceStore, stopTimesStore, tripsStore,
                frequenciesReader, stopTimesReader);
        final List<TripStop> stops = directory.getStopsOnTripInRange(
                new TripId("11367651"),
                new TransitTime((byte) 5, (byte) 45, (byte) 0),
                new TransitTime((byte) 5, (byte) 48, (byte) 0));
        Assert.assertEquals(2, stops.size());
    }

    @Test
    public void testGetsStopsOnTripWithFrequencies() throws Exception {
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = new MapRangedStore<TripSequenceKey, TripStop>(
                        new TreeMap<>());
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = new MapRangedStore<StopTimeKey, TripStop>(new TreeMap<>());
        final Store<TripIdKey, TripId> tripsStore
                = new MapStore<>(new HashMap<>());

        final Reader frequenciesReader = new StringReader(
                "trip_id,start_time,end_time,headway_secs\n"
                        + "11367651,05:00:00,06:00:00,1200");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n"
                + "11367651,05:45:05,05:45:05,26700,4,\"\",0,0,549.2,1\n"
                        + "11367651,05:47:06,05:47:06,26702,13,\"\",0,0,1879.6,1\n"
                + "11367652,06:02:06,06:02:06,26702,13,\"\",0,0,1879.6,1");

        final StopTimesDirectory directory
                = new GTFSReadingStopTimesDirectory(
                        tripSequenceStore, stopTimesStore, tripsStore,
                        frequenciesReader, stopTimesReader);
        final List<TripStop> stops = directory.getStopsOnTripInRange(
                new TripId("11367651", "05:40:00"),
                new TransitTime((byte) 5, (byte) 39, (byte) 0),
                new TransitTime((byte) 5, (byte) 43, (byte) 0));

        Assert.assertEquals(2, stops.size());
    }

    @Test
    public void testGetsStopsAtStopWithFrequencies() throws Exception {
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = new MapRangedStore<TripSequenceKey, TripStop>(
                        new TreeMap<>());
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = new MapRangedStore<StopTimeKey, TripStop>(new TreeMap<>());
        final Store<TripIdKey, TripId> tripsStore
                = new MapStore<>(new HashMap<>());

        final Reader frequenciesReader = new StringReader(
                "trip_id,start_time,end_time,headway_secs\n"
                        + "11367651,05:00:00,06:00:00,1200");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n"
                + "11367651,05:45:05,05:45:05,26700,4,\"\",0,0,549.2,1\n"
                        + "11367651,05:47:06,05:47:06,26702,13,\"\",0,0,1879.6,1\n"
                + "11367652,06:02:06,06:02:06,26702,13,\"\",0,0,1879.6,1");

        final StopTimesDirectory directory
                = new GTFSReadingStopTimesDirectory(
                        tripSequenceStore, stopTimesStore, tripsStore,
                        frequenciesReader, stopTimesReader);
        final List<TripStop> stops = directory.getStopsAtStopInRange(
                "26702", new TransitTime((byte) 5, (byte) 0, (byte) 0),
                new TransitTime((byte) 7, (byte) 0, (byte) 0));
        Assert.assertEquals(4, stops.size());
    }

}
