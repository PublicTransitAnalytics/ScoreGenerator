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
import com.bitvantage.bitvantagecaching.Store;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Directory for looking up the data pertaining to a trip.
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingTripDetailsDirectory implements TripDetailsDirectory {

    private final Store<TripGroupKey, TripDetails> tripDetailsStore;

    public GTFSReadingTripDetailsDirectory(
            final Store<TripGroupKey, TripDetails> tripDetailsStore,
            final Reader tripReader)
            throws IOException, InterruptedException {

        this.tripDetailsStore = tripDetailsStore;
        try {
            if (tripDetailsStore.isEmpty()) {
                parseTripsFile(tripReader);
            }
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public TripDetails getTripDetails(final TripGroupKey key)
            throws InterruptedException {
        try {
            return tripDetailsStore.get(key);
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public Set<TripDetails> getAllTripDetails() throws InterruptedException {
        try {
            return ImmutableSet.copyOf(tripDetailsStore.getValues());
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    private void parseTripsFile(final Reader tripReader) throws IOException,
            InterruptedException {

        final CSVParser tripParser = new CSVParser(
                tripReader, CSVFormat.DEFAULT.withHeader());

        final List<CSVRecord> tripRecords = tripParser.getRecords();
        for (CSVRecord record : tripRecords) {
            final String rawTripId = record.get("trip_id");
            final String routeId = record.get("route_id");
            final String serviceType = record.get("service_id");
            populateTripDetail(rawTripId, routeId, serviceType);
        }
    }

    private void populateTripDetail(final String tripId, final String routeId,
                                    final String serviceType)
            throws InterruptedException {
        final TripDetails details
                = new TripDetails(tripId, routeId, serviceType);
        try {
            tripDetailsStore.put(new TripGroupKey(tripId), details);
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

}
