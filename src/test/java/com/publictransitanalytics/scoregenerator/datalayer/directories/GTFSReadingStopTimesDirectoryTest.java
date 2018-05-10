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
import com.bitvantage.bitvantagecaching.mocks.MapRangedStore;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripSequence;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStops;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripSequenceKey;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingStopTimesDirectoryTest {

    @Test
    public void testIncludesPriorArrival() throws Exception {
        final RangedStore<TimeKey, TripSequence> arrivalTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TimeKey, TripSequence> departureTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TripSequenceKey, TripStop> tripStopStore
                = new MapRangedStore(new TreeMap<>());

        final Reader frequenciesReader = new StringReader("");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n" +
                "11367651,05:45:00,05:45:00,26700,4,\"\",0,0,549.2,1\n" +
                "11367651,05:46:00,05:47:00,26702,13,\"\",0,0,1879.6,1\n" +
                "11367651,06:00:00,06:00:00,26703,14,\"\",0,0,1879.6,1\n");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                arrivalTimeStore, departureTimeStore, tripStopStore,
                frequenciesReader, stopTimesReader);
        final Set<TripStops> tripStops = directory.getAllTripStops(
                new TransitTime(5, 46, 30), new TransitTime(6, 0, 0));
        Assert.assertEquals(1, tripStops.size());
        Assert.assertEquals(2, tripStops.iterator().next().getStops().size());
    }

    @Test
    public void testIncludesLaterDeparture() throws Exception {
        final RangedStore<TimeKey, TripSequence> arrivalTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TimeKey, TripSequence> departureTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TripSequenceKey, TripStop> tripStopStore
                = new MapRangedStore(new TreeMap<>());

        final Reader frequenciesReader = new StringReader("");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n" +
                "11367651,05:45:00,05:45:00,26700,4,\"\",0,0,549.2,1\n" +
                "11367651,05:46:06,05:47:00,26702,13,\"\",0,0,1879.6,1\n" +
                "11367651,06:00:00,06:00:00,26703,14,\"\",0,0,1879.6,1\n");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                arrivalTimeStore, departureTimeStore, tripStopStore,
                frequenciesReader, stopTimesReader);
        final Set<TripStops> tripStops = directory.getAllTripStops(
                new TransitTime(5, 45, 0), new TransitTime(5, 46, 30));
        Assert.assertEquals(1, tripStops.size());
        Assert.assertEquals(2, tripStops.iterator().next().getStops().size());
    }

    @Test
    public void testIncludesMissingTime() throws Exception {
        final RangedStore<TimeKey, TripSequence> arrivalTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TimeKey, TripSequence> departureTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TripSequenceKey, TripStop> tripStopStore
                = new MapRangedStore(new TreeMap<>());

        final Reader frequenciesReader = new StringReader("");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n" +
                "11367651,05:45:00,05:45:00,26700,4,\"\",0,0,549.2,1\n" +
                "11367651,,,26702,13,\"\",0,0,1879.6,1\n" +
                "11367651,06:00:00,06:00:00,26703,14,\"\",0,0,1879.6,1\n");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                arrivalTimeStore, departureTimeStore, tripStopStore,
                frequenciesReader, stopTimesReader);
        final Set<TripStops> tripStops = directory.getAllTripStops(
                new TransitTime(5, 45, 0), new TransitTime(6, 0, 0));
        Assert.assertEquals(1, tripStops.size());
        Assert.assertEquals(3, tripStops.iterator().next().getStops().size());
    }

    @Test
    public void testMakesTripsForFrequencies() throws Exception {
        final RangedStore<TimeKey, TripSequence> arrivalTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TimeKey, TripSequence> departureTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TripSequenceKey, TripStop> tripStopStore
                = new MapRangedStore(new TreeMap<>());

        final Reader frequenciesReader = new StringReader(
                "trip_id,start_time,end_time,headway_secs\n" +
                "11367651,05:00:00,06:00:00,1200");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n" +
                "11367651,05:45:00,05:45:00,26700,4,\"\",0,0,549.2,1\n" +
                "11367651,,,26702,13,\"\",0,0,1879.6,1\n" +
                "11367651,06:00:00,06:00:00,26703,14,\"\",0,0,1879.6,1\n");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                arrivalTimeStore, departureTimeStore, tripStopStore,
                frequenciesReader, stopTimesReader);
        final Set<TripStops> tripStops = directory.getAllTripStops(
                new TransitTime(5, 0, 0), new TransitTime(6, 0, 0));
        Assert.assertEquals(3, tripStops.size());
    }

    @Test
    public void testMakesMultipleTrips() throws Exception {
        final RangedStore<TimeKey, TripSequence> arrivalTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TimeKey, TripSequence> departureTimeStore
                = new MapRangedStore(new TreeMap<>());
        final RangedStore<TripSequenceKey, TripStop> tripStopStore
                = new MapRangedStore(new TreeMap<>());

        final Reader frequenciesReader = new StringReader("");
        final Reader stopTimesReader = new StringReader(
                "trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled,fare_period_id\n" +
                "11367651,05:45:00,05:45:00,26700,4,\"\",0,0,549.2,1\n" +
                "11367652,05:46:06,05:47:00,26702,13,\"\",0,0,1879.6,1\n" +
                "11367651,06:00:00,06:00:00,26703,14,\"\",0,0,1879.6,1\n");

        final StopTimesDirectory directory = new GTFSReadingStopTimesDirectory(
                arrivalTimeStore, departureTimeStore, tripStopStore,
                frequenciesReader, stopTimesReader);
        final Set<TripStops> tripStops = directory.getAllTripStops(
                new TransitTime(5, 45, 0), new TransitTime(6, 0, 0));
        Assert.assertEquals(2, tripStops.size());
    }
}
