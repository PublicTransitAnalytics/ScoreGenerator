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

import com.bitvantage.bitvantagecaching.GsonSerializer;
import com.bitvantage.bitvantagecaching.IntegerSerializer;
import com.bitvantage.bitvantagecaching.StoreBackedRangedKeyStore;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Serializer;
import com.bitvantage.bitvantagecaching.Store;
import com.publictransitanalytics.scoregenerator.StoreFactory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.StopDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripSequence;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.DateKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.RouteIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopTimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripSequenceKey;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationKey;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationTimeKey;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;

/**
 *
 * @author Public Transit Analytics
 */
public class LocalServiceDataDirectory implements ServiceDataDirectory {

    private static final String GTFS_DIRECTORY = "gtfs";

    private static final String STOP_DETAILS_STORE = "stop_details_store";

    private static final String WALKING_TIME_STORE = "walking_time_store";
    private static final String MAX_WALKING_TIME_STORE
            = "max_walking_time_store";
    private static final String SERVICE_TYPES_STORE = "service_types_store";
    private static final String ROUTE_DETAILS_STORE = "route_details_store";
    private static final String TRIP_DETAILS_STORE = "trip_details_store";
    private static final String ARRIVAL_TIMES_STORE = "arrival_times_store";
    private static final String DEPARTURE_TIMES_STORE = "departure_times_store";
    private static final String TRIP_STOP_STORE = "trip_stop_store";

    private static final String STOPS_FILE = "stops.txt";
    private static final String STOP_TIMES_FILE = "stop_times.txt";
    private static final String FREQUENCIES_FILE = "frequencies.txt";
    private static final String CALENDAR_FILE = "calendar.txt";
    private static final String CALENDAR_DATES_FILE = "calendar_dates.txt";
    private static final String ROUTES_FILE = "routes.txt";
    private static final String TRIPS_FILE = "trips.txt";

    @Getter
    private final TripDetailsDirectory tripDetailsDirectory;
    @Getter
    private final RouteDetailsDirectory routeDetailsDirectory;
    @Getter
    private final ServiceTypeCalendar serviceTypeCalendar;
    @Getter
    private final StopDetailsDirectory stopDetailsDirectory;
    @Getter
    private final StopTimesDirectory stopTimesDirectory;
    @Getter
    private final StoreBackedRangedKeyStore<LocationTimeKey> walkingTimeStore;
    @Getter
    private final Store<LocationKey, Integer> maxWalkingTimeStore;

    public LocalServiceDataDirectory(final Path root, final String files,
                                     final StoreFactory storeFactory)
            throws InterruptedException, IOException {
        stopDetailsDirectory = buildStopDetailsDirectory(storeFactory, root,
                                                         files);
        serviceTypeCalendar
                = buildServiceTypeCalendar(storeFactory, root, files);
        tripDetailsDirectory
                = buildTripDetailsDirectory(storeFactory, root, files);
        routeDetailsDirectory
                = buildRouteDetailsDirectory(storeFactory, root, files);
        stopTimesDirectory
                = buildStopTimesDirectory(storeFactory, root, files);

        final Path walkingTimeStorePath = root.resolve(files)
                .resolve(WALKING_TIME_STORE);
        walkingTimeStore = storeFactory
                .<LocationTimeKey>getRangedKeyStore(
                        walkingTimeStorePath,
                        new LocationTimeKey.Materializer());
        final Path maxWalkingTimeStorePath = root.resolve(files)
                .resolve(MAX_WALKING_TIME_STORE);
        maxWalkingTimeStore = storeFactory.<LocationKey, Integer>getStore(
                maxWalkingTimeStorePath, new IntegerSerializer());
    }

    private static StopDetailsDirectory buildStopDetailsDirectory(
            final StoreFactory storeFactory, final Path baseDirectory,
            final String revision) throws InterruptedException, IOException {
        final Serializer<StopDetails> serializer
                = new GsonSerializer<>(StopDetails.class);
        final Store<StopIdKey, StopDetails> stopDetailsStore
                = storeFactory.<StopIdKey, StopDetails>getStore(
                        baseDirectory.resolve(revision).resolve(
                                STOP_DETAILS_STORE), serializer);

        final Reader stopDetailsReader = new FileReader(
                baseDirectory.resolve(revision).resolve(GTFS_DIRECTORY).resolve(
                        STOPS_FILE).toFile());

        final GTFSReadingStopDetailsDirectory stopDetailsDirectory
                = new GTFSReadingStopDetailsDirectory(stopDetailsStore,
                                                      stopDetailsReader);
        return stopDetailsDirectory;
    }

    private static ServiceTypeCalendar buildServiceTypeCalendar(
            final StoreFactory storeFactory, final Path root,
            final String revision) throws IOException, InterruptedException {
        final Serializer<ServiceSet> serializer
                = new GsonSerializer<>(ServiceSet.class);
        final Store<DateKey, ServiceSet> serviceTypesStore
                = storeFactory.<DateKey, ServiceSet>getStore(
                        root.resolve(revision).resolve(
                                SERVICE_TYPES_STORE), serializer);

        final Reader calendarReader = new FileReader(
                root.resolve(revision).resolve(GTFS_DIRECTORY).
                        resolve(CALENDAR_FILE).toFile());
        final Reader calendarDatesReader = new FileReader(root.
                resolve(revision).resolve(GTFS_DIRECTORY).
                resolve(CALENDAR_DATES_FILE).toFile());

        final GTFSReadingServiceTypeCalendar serviceTypeCalendar
                = new GTFSReadingServiceTypeCalendar(
                        serviceTypesStore, calendarReader, calendarDatesReader);
        return serviceTypeCalendar;
    }

    private static TripDetailsDirectory buildTripDetailsDirectory(
            final StoreFactory storeFactory, final Path root,
            final String revision) throws IOException, InterruptedException {
        final Serializer<TripDetails> serializer
                = new GsonSerializer<>(TripDetails.class);
        final Store<TripGroupKey, TripDetails> tripDetailsStore
                = storeFactory.<TripGroupKey, TripDetails>getStore(
                        root.resolve(revision).resolve(
                                TRIP_DETAILS_STORE), serializer);

        final Reader tripReader = new FileReader(root.resolve(revision)
                .resolve(GTFS_DIRECTORY).resolve(TRIPS_FILE).toFile());
        final TripDetailsDirectory tripDetailsDirectory
                = new GTFSReadingTripDetailsDirectory(tripDetailsStore,
                                                      tripReader);
        return tripDetailsDirectory;
    }

    private static StopTimesDirectory buildStopTimesDirectory(
            final StoreFactory storeFactory, final Path root,
            final String revision) throws IOException, InterruptedException {
        final Path arrivalTimeStorePath
                = root.resolve(revision).resolve(ARRIVAL_TIMES_STORE);
        final Path departureTimeStorePath
                = root.resolve(revision).resolve(DEPARTURE_TIMES_STORE);
        final Serializer<TripSequence> tripSequenceSerializer
                = new GsonSerializer<>(TripSequence.class);
        final TimeKey.Materializer timeKeyMaterializer
                = new TimeKey.Materializer();

        final RangedStore<TimeKey, TripSequence> arrivalTimeStore
                = storeFactory.<TimeKey, TripSequence>getRangedStore(
                        arrivalTimeStorePath, timeKeyMaterializer,
                        tripSequenceSerializer);
        final RangedStore<TimeKey, TripSequence> departureTimeStore
                = storeFactory.<TimeKey, TripSequence>getRangedStore(
                        departureTimeStorePath, timeKeyMaterializer,
                        tripSequenceSerializer);

        final Path tripStopStorePath = root.resolve(revision)
                .resolve(TRIP_STOP_STORE);
        final Serializer<TripStop> tripStopSerializer
                = new GsonSerializer<>(TripStop.class);
        final RangedStore<TripSequenceKey, TripStop> tripStopsStore
                = storeFactory.<TripSequenceKey, TripStop>getRangedStore(
                        tripStopStorePath, new TripSequenceKey.Materializer(),
                        tripStopSerializer);

        final Path frequenciesPath = root.resolve(revision).resolve(
                GTFS_DIRECTORY).resolve(FREQUENCIES_FILE);
        final Reader frequenciesReader;
        if (Files.exists(frequenciesPath)) {
            frequenciesReader = new FileReader(frequenciesPath.toFile());
        } else {
            frequenciesReader = new StringReader("");
        }
        final Reader stopTimesReader = new FileReader(root.resolve(revision)
                .resolve(GTFS_DIRECTORY).resolve(STOP_TIMES_FILE).toFile());

        final GTFSReadingStopTimesDirectory stopTimesDirectory
                = new GTFSReadingStopTimesDirectory(
                        arrivalTimeStore, departureTimeStore, tripStopsStore,
                        frequenciesReader, stopTimesReader);
        return stopTimesDirectory;
    }

    private static RouteDetailsDirectory buildRouteDetailsDirectory(
            final StoreFactory storeFactory, final Path root,
            final String revision) throws IOException, InterruptedException {
        final Serializer<RouteDetails> serializer
                = new GsonSerializer<>(RouteDetails.class);
        final Store<RouteIdKey, RouteDetails> routeDetailsStore
                = storeFactory.<RouteIdKey, RouteDetails>getStore(
                        root.resolve(revision).resolve(ROUTE_DETAILS_STORE),
                        serializer);

        final Reader routeReader = new FileReader(root
                .resolve(revision).resolve(GTFS_DIRECTORY)
                .resolve(ROUTES_FILE).toFile());

        final RouteDetailsDirectory routeDetailsDirectory
                = new GTFSReadingRouteDetailsDirectory(
                        routeDetailsStore, routeReader);
        return routeDetailsDirectory;
    }

}
