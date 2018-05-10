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

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.FrequencyRecord;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RawTripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripSequenceKey;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripSequence;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStops;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TimeKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static final TimeKey EARLIEST_TIME_KEY
            = TimeKey.getMinKey(TransitTime.MIN_TRANSIT_TIME);
    private static final TimeKey LATEST_TIME_KEY
            = TimeKey.getMaxKey(TransitTime.MAX_TRANSIT_TIME);

    private final RangedStore<TimeKey, TripSequence> arrivalTimeStore;
    private final RangedStore<TimeKey, TripSequence> departureTimeStore;
    private final RangedStore<TripSequenceKey, TripStop> tripStopStore;

    public GTFSReadingStopTimesDirectory(
            final RangedStore<TimeKey, TripSequence> arrivalTimeStore,
            final RangedStore<TimeKey, TripSequence> departureTimeStore,
            final RangedStore<TripSequenceKey, TripStop> tripStopStore,
            final Reader frequenciesReader, final Reader stopTimesReader)
            throws IOException, InterruptedException {

        this.arrivalTimeStore = arrivalTimeStore;
        this.departureTimeStore = departureTimeStore;
        this.tripStopStore = tripStopStore;

        try {
            if (arrivalTimeStore.isEmpty() || departureTimeStore.isEmpty()) {
                final SetMultimap<String, FrequencyRecord> frequencies
                        = parseFrequenciesFile(frequenciesReader);
                parseStopTimesFile(frequencies, stopTimesReader);
            }
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public Set<TripStops> getAllTripStops(final TransitTime startTime,
                                          final TransitTime endTime)
            throws InterruptedException {
        try {
            final NavigableMap<TimeKey, TripSequence> arrivalMap
                    = arrivalTimeStore.getValuesInRange(
                            EARLIEST_TIME_KEY, TimeKey.getMaxKey(endTime));
            final NavigableMap<TimeKey, TripSequence> departureMap
                    = departureTimeStore.getValuesInRange(
                            TimeKey.getMinKey(startTime), LATEST_TIME_KEY);

            final Map<TripSequence, TransitTime> arrivalTimeMap
                    = arrivalMap.entrySet().stream().collect(Collectors.toMap(
                            entry -> entry.getValue(),
                            entry -> entry.getKey().getTime()));
            final Map<TripSequence, TransitTime> departureTimeMap
                    = departureMap.entrySet().stream().collect(Collectors.toMap(
                            entry -> entry.getValue(),
                            entry -> entry.getKey().getTime()));

            final Set<TripSequence> locations = Sets.intersection(
                    arrivalTimeMap.keySet(), departureTimeMap.keySet());
            final Map<TripId, Integer> minSequences = new HashMap<>();
            final Map<TripId, Integer> maxSequences = new HashMap<>();
            for (final TripSequence location : locations) {
                final int sequence = location.getSequence();
                final TripId tripId = location.getTripId();

                if (maxSequences.containsKey(tripId)) {
                    final int current = maxSequences.get(tripId);
                    if (sequence > current) {
                        maxSequences.replace(tripId, current, sequence);
                    }
                } else {
                    maxSequences.put(tripId, sequence);
                }
                if (minSequences.containsKey(tripId)) {
                    final int current = minSequences.get(tripId);
                    if (sequence < current) {
                        minSequences.replace(tripId, current, sequence);
                    }
                } else {
                    minSequences.put(tripId, sequence);
                }
            }

            final ImmutableSet.Builder<TripStops> builder
                    = ImmutableSet.builder();
            for (final TripId tripId : maxSequences.keySet()) {
                final TripSequenceKey minKey = new TripSequenceKey(
                        tripId, minSequences.get(tripId));
                final TripSequenceKey maxKey = new TripSequenceKey(
                        tripId, maxSequences.get(tripId));
                final NavigableMap<TripSequenceKey, TripStop> stopMap
                        = tripStopStore.getValuesInRange(minKey, maxKey);
                final List<TripStop> stops
                        = ImmutableList.copyOf(stopMap.values());
                builder.add(new TripStops(stops, tripId));
            }
            return builder.build();

        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    private SetMultimap<String, FrequencyRecord> parseFrequenciesFile(
            final Reader frequenciesReader) throws
            FileNotFoundException, IOException {
        final ImmutableSetMultimap.Builder<String, FrequencyRecord> builder
                = ImmutableSetMultimap.builder();

        final CSVParser frequenciesParser = new CSVParser(
                frequenciesReader, CSVFormat.DEFAULT.withHeader());
        final List<CSVRecord> frequenciesRecords
                = frequenciesParser.getRecords();

        for (CSVRecord record : frequenciesRecords) {
            final String tripId = record.get("trip_id");

            final FrequencyRecord frequencyRecord = new FrequencyRecord(
                    tripId, TransitTime.parse(record.get("start_time")),
                    TransitTime.parse(record.get("end_time")), Duration
                    .ofSeconds(Long.parseLong(record.get("headway_secs"))));
            builder.put(tripId, frequencyRecord);
        }
        return builder.build();
    }

    private void parseStopTimesFile(
            final SetMultimap<String, FrequencyRecord> frequencyRecordMap,
            final Reader stopTimesReader)
            throws FileNotFoundException, IOException, InterruptedException {

        final CSVParser parser = new CSVParser(stopTimesReader,
                                               CSVFormat.DEFAULT.withHeader());

        final SortedSetMultimap<String, RawTripStop> rawTripMap = TreeMultimap
                .create(Comparator.naturalOrder(),
                        (stop1, stop2) -> Integer.compare(
                                stop1.getSequence(), stop2.getSequence()));

        final Iterator<CSVRecord> stopTimesIter = parser.iterator();
        while (stopTimesIter.hasNext()) {
            final CSVRecord record = stopTimesIter.next();
            final String rawTripId = record.get("trip_id");
            final int stopSequence = Integer.valueOf(
                    record.get("stop_sequence"));
            final String stopId = record.get("stop_id");
            final String arrivalTimeString = record.get("arrival_time");
            final TransitTime arrivalTime = (arrivalTimeString.isEmpty()) ? null
                    : TransitTime.parse(arrivalTimeString);
            final String departureTimeString = record.get("departure_time");
            final TransitTime departureTime = (departureTimeString.isEmpty())
                    ? null : TransitTime.parse(departureTimeString);

            if (frequencyRecordMap.containsKey(rawTripId)) {
                final RawTripStop rawTripStop = new RawTripStop(
                        arrivalTime, departureTime, stopId, rawTripId,
                        stopSequence);
                rawTripMap.put(rawTripId, rawTripStop);
            } else {
                final TripId tripId = new TripId(rawTripId);

                try {
                    insertToStores(stopSequence, tripId, stopId, arrivalTime,
                                   departureTime);
                } catch (final BitvantageStoreException e) {
                    throw new ScoreGeneratorFatalException(e);
                }
            }
        }
        for (final String rawTripId : rawTripMap.keySet()) {
            final Set<FrequencyRecord> frequencyRecords
                    = frequencyRecordMap.get(rawTripId);
            for (final FrequencyRecord frequencyRecord : frequencyRecords) {

                TransitTime recurringTime = frequencyRecord.getStartTime();
                while (recurringTime.isBefore(frequencyRecord.getEndTime())) {
                    final TransitTime baseArrivalTime = rawTripMap
                            .get(rawTripId).first().getArrivalTime();
                    final TransitTime baseDepartureTime = rawTripMap
                            .get(rawTripId).first().getDepartureTime();
                    final TripId tripId = new TripId(
                            rawTripId, recurringTime.toString());

                    for (final RawTripStop rawTripStop : rawTripMap.get(
                            rawTripId)) {
                        final TransitTime arrivalTime;
                        final TransitTime rawArrivalTime
                                = rawTripStop.getArrivalTime();
                        if (rawArrivalTime != null) {
                            arrivalTime = recurringTime.plus(
                                    TransitTime.durationBetween(
                                            baseArrivalTime, rawArrivalTime));
                        } else {
                            arrivalTime = null;
                        }

                        final TransitTime departureTime;
                        final TransitTime rawDepartureTime
                                = rawTripStop.getDepartureTime();
                        if (rawDepartureTime != null) {
                            departureTime = recurringTime.plus(
                                    TransitTime.durationBetween(
                                            baseDepartureTime, 
                                            rawDepartureTime));
                        } else {
                            departureTime = null;
                        }

                        final int stopSequence = rawTripStop.getSequence();
                        final String stopId = rawTripStop.getStopId();

                        try {
                            insertToStores(stopSequence, tripId, stopId,
                                           arrivalTime, departureTime);
                        } catch (final BitvantageStoreException e) {
                            throw new ScoreGeneratorFatalException(e);
                        }
                    }
                    recurringTime = recurringTime.plus(
                            frequencyRecord.getInterval());
                }
            }
        }
    }

    private void insertToStores(final int stopSequence, final TripId tripId,
                                final String stopId,
                                final TransitTime arrivalTime,
                                final TransitTime departureTime)
            throws BitvantageStoreException, InterruptedException {
        final TripSequence tripSequence
                = new TripSequence(stopSequence, tripId);

        if (arrivalTime != null) {
            final TimeKey arrivalKey = TimeKey.getWriteKey(arrivalTime);
            arrivalTimeStore.put(arrivalKey, tripSequence);
        }
        if (departureTime != null) {
            final TimeKey departureKey
                    = TimeKey.getWriteKey(departureTime);
            departureTimeStore.put(departureKey, tripSequence);
        }
        final TripSequenceKey sequenceKey = new TripSequenceKey(tripId,
                                                                stopSequence);
        final TripStop tripStop = new TripStop(arrivalTime, departureTime,
                                               stopId, stopSequence);

        tripStopStore.put(sequenceKey, tripStop);
    }

}
