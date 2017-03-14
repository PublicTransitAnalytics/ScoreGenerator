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
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.RouteIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Directory that surfaces information from the GTFS routes file.
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingRouteDetailsDirectory implements RouteDetailsDirectory {

    private final Store<RouteIdKey, RouteDetails> routeDetailsStore;

    public GTFSReadingRouteDetailsDirectory(
            final Store<RouteIdKey, RouteDetails> routeDetailsStore,
            final Reader routeReader) throws IOException {

        this.routeDetailsStore = routeDetailsStore;
        if (routeDetailsStore.isEmpty()) {
            parseRoutesFile(routeDetailsStore, routeReader);
        }
    }

    @Override
    public RouteDetails getRouteDetails(final String routeId) {
        return routeDetailsStore.get(new RouteIdKey(routeId));
    }

    private static void parseRoutesFile(
            final Store<RouteIdKey, RouteDetails> store,
            final Reader routeReader) throws IOException {

        final CSVParser routeParser = new CSVParser(
                routeReader, CSVFormat.DEFAULT.withHeader());
        final List<CSVRecord> routeRecords = routeParser.getRecords();
        for (final CSVRecord record : routeRecords) {
            String routeId = record.get("route_id");
            String routeShortName = record.get("route_short_name");
            String routeDescription = record.get("route_desc");
            populateRouteDetail(routeId, routeShortName, routeDescription,
                                store);
        }
    }

    private static void populateRouteDetail(
            final String routeId, final String routeShortName,
            final String routeDescription,
            final Store<RouteIdKey, RouteDetails> store) {
        final RouteDetails details = new RouteDetails(routeShortName,
                                                      routeDescription);
        store.put(new RouteIdKey(routeId), details);
    }

}
