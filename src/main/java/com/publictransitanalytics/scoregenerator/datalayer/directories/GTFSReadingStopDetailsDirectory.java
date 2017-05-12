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
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.Coordinate;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.StopDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopIdKey;
import com.google.common.collect.Multiset;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Directory of stop information generated from GFTS data.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class GTFSReadingStopDetailsDirectory implements StopDetailsDirectory {

    private final Store<StopIdKey, StopDetails> stopDetailsStore;

    public GTFSReadingStopDetailsDirectory(
            final Store<StopIdKey, StopDetails> stopDetailsStore,
            final Reader stopDetailsReader)
            throws IOException, InterruptedException {

        this.stopDetailsStore = stopDetailsStore;
        try {
            if (stopDetailsStore.isEmpty()) {
                log.info("Building stop details directory.");

                final CSVParser parser = new CSVParser(
                        stopDetailsReader, CSVFormat.DEFAULT.withHeader());
                final List<CSVRecord> stopDetailsRecords = parser.getRecords();
                for (CSVRecord record : stopDetailsRecords) {
                    final double latitude = Double.valueOf(record
                            .get("stop_lat"));
                    final double longitude = Double.valueOf(record.get(
                            "stop_lon"));
                    final String stopId = record.get("stop_id");
                    final StopDetails stopDetails = new StopDetails(
                            stopId, record.get("stop_name"),
                            new Coordinate(latitude, longitude));
                    stopDetailsStore.put(new StopIdKey(stopId), stopDetails);
                }
            }
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public StopDetails getDetails(final String stopId)
            throws InterruptedException {
        try {
            return stopDetailsStore.get(new StopIdKey(stopId));
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public Multiset<StopDetails> getAllStopDetails()
            throws InterruptedException {
        try {
            return stopDetailsStore.getValues();
        } catch (final BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

}
