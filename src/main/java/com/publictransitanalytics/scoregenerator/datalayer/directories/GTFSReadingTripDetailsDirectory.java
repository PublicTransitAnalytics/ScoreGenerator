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
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
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
            final Reader tripReader) throws IOException {

        this.tripDetailsStore = tripDetailsStore;
        if (tripDetailsStore.isEmpty()) {
            parseTripsFile(tripReader);
        }
    }

    @Override
    public TripDetails getTripDetails(final TripGroupKey key) {
        return tripDetailsStore.get(key);
    }

    private void parseTripsFile(final Reader tripReader) throws IOException {

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
                                    final String serviceType) {
        final TripDetails details
                = new TripDetails(tripId, routeId, serviceType);
        tripDetailsStore.put(new TripGroupKey(tripId), details);
    }

}
