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

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.FrequencyRecord;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RawTripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopTimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripSequenceKey;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Directory that tracks where transit vehicles are stopping.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class GTFSReadingStopTimesDirectory implements StopTimesDirectory {

    final RangedStore<TripSequenceKey, TripStop> tripSequenceStore;
    final RangedStore<StopTimeKey, TripStop> stopTimesStore;
    final Store<TripIdKey, TripId> tripsStore;

    public GTFSReadingStopTimesDirectory(
            final RangedStore<TripSequenceKey, TripStop> tripSequenceStore,
            final RangedStore<StopTimeKey, TripStop> stopTimesStore,
            final Store<TripIdKey, TripId> tripsStore,
            final Reader frequenciesReader, final Reader stopTimesReader)
            throws IOException {

        final ImmutableMap.Builder<String, FrequencyRecord> builder
                = ImmutableMap.builder();
        this.tripSequenceStore = tripSequenceStore;
        this.stopTimesStore = stopTimesStore;
        this.tripsStore = tripsStore;

        if (tripSequenceStore.isEmpty() || stopTimesStore.isEmpty()
                || tripsStore.isEmpty()) {
            parseFrequenciesFile(builder, frequenciesReader);
            parseStopTimesFile(builder.build(), stopTimesReader);
        }
    }

    @Override
    public List<TripStop> getStopsAtStopInRange(
            final String stopId, final TransitTime startTime,
            final TransitTime endTime) {
        return stopTimesStore.getValuesInRange(
                StopTimeKey.getMinKey(stopId, startTime),
                StopTimeKey.getMaxKey(stopId, endTime));
    }

    @Override
    public List<TripStop> getStopsOnTripInRange(
            final TripId tripId, final TransitTime startTime,
            final TransitTime endTime) {
        final TripIdKey baseKey = new TripIdKey(tripId.getRawTripId(), tripId
                                                .getQualifier());
        return tripSequenceStore.getValuesInRange(
                new TripSequenceKey(baseKey, startTime, 0),
                new TripSequenceKey(baseKey, endTime, Integer.MAX_VALUE));
    }

    @Override
    public List<TripStop> getSubsequentStopsOnTrip(
            final TripId tripId, final TransitTime startTime) {
        return getStopsOnTripInRange(tripId, startTime,
                                     TransitTime.MAX_TRANSIT_TIME);
    }

    @Override
    public Set<TripId> getTripIds() {
        return ImmutableSet.copyOf(tripsStore.getValues());
    }

    private void parseFrequenciesFile(
            final ImmutableMap.Builder<String, FrequencyRecord> builder,
            final Reader frequenciesReader) throws
            FileNotFoundException, IOException {

        final CSVParser frequenciesParser
                = new CSVParser(frequenciesReader,
                                CSVFormat.DEFAULT.withHeader());
        final List<CSVRecord> frequenciesRecords = frequenciesParser
                .getRecords();

        for (CSVRecord record : frequenciesRecords) {
            final String tripId = record.get("trip_id");

            final FrequencyRecord frequencyRecord = new FrequencyRecord(
                    tripId, TransitTime.parse(record.get("start_time")),
                    TransitTime.parse(record.get("end_time")), Duration
                    .ofSeconds(Long.parseLong(record.get("headway_secs"))));
            builder.put(tripId, frequencyRecord);
        }
    }

    private void parseStopTimesFile(
            final ImmutableMap<String, FrequencyRecord> frequencyRecordMap,
            final Reader stopTimesReader)
            throws FileNotFoundException, IOException {

        final CSVParser parser = new CSVParser(stopTimesReader,
                                               CSVFormat.DEFAULT.withHeader());

        final SortedSetMultimap<String, RawTripStop> rawTripMap = TreeMultimap
                .create((final String a, final String b) -> a.compareTo(b),
                        (final RawTripStop a, final RawTripStop b) -> Integer
                        .compare(a.getSequence(), b.getSequence()));

        final Iterator<CSVRecord> stopTimesIter = parser.iterator();
        while (stopTimesIter.hasNext()) {
            final CSVRecord record = stopTimesIter.next();
            final String rawTripId = record.get("trip_id");
            final int stopSequence = Integer
                    .valueOf(record.get("stop_sequence"));
            final String stopId = record.get("stop_id");
            final TransitTime stopTime = TransitTime.parse(record.get(
                    "arrival_time"));

            if (frequencyRecordMap.containsKey(rawTripId)) {
                final RawTripStop rawTripStop
                        = new RawTripStop(stopTime, stopId, rawTripId,
                                          stopSequence);
                rawTripMap.put(rawTripId, rawTripStop);
            } else {
                final TripId tripId = new TripId(rawTripId);
                final TripStop tripStop = new TripStop(stopTime, stopId,
                                                       tripId, stopSequence);
                final TripIdKey tripIdKey = new TripIdKey(rawTripId);
                tripsStore.put(tripIdKey, tripId);
                tripSequenceStore.put(new TripSequenceKey(
                        tripIdKey, stopTime, stopSequence),
                                      tripStop);
                stopTimesStore.put(StopTimeKey.getWriteKey(stopId, stopTime),
                                   tripStop);
            }
        }
        for (final String rawTripId : rawTripMap.keySet()) {
            final FrequencyRecord frequencyRecord = frequencyRecordMap.get(
                    rawTripId);

            TransitTime recurringTime = frequencyRecord.getStartTime();
            while (recurringTime.isBefore(frequencyRecord.getEndTime())) {
                final TransitTime baseTime = rawTripMap.get(rawTripId).first()
                        .getTime();
                final TripId tripId = new TripId(rawTripId, recurringTime
                                                 .toString());

                for (final RawTripStop rawTripStop : rawTripMap.get(rawTripId)) {
                    final TransitTime stopTime = recurringTime.plus(
                            TransitTime.durationBetween(baseTime, rawTripStop
                                                        .getTime()));
                    final int stopSequence = rawTripStop.getSequence();
                    final String stopId = rawTripStop.getStopId();

                    final TripStop tripStop = new TripStop(stopTime, stopId,
                                                           tripId, stopSequence);

                    final TripIdKey tripIdKey = new TripIdKey(
                            tripId.getRawTripId(), tripId.getQualifier());
                    
                    tripsStore.put(tripIdKey, tripId);
                    tripSequenceStore.put(new TripSequenceKey(
                            tripIdKey, stopTime, stopSequence), tripStop);
                    stopTimesStore
                            .put(StopTimeKey.getWriteKey(stopId, stopTime),
                                 tripStop);
                }
                recurringTime = recurringTime
                        .plus(frequencyRecord.getInterval());
            }
        }
    }
}
