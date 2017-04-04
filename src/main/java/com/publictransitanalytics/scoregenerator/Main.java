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
package com.publictransitanalytics.scoregenerator;

import com.bitvantage.bitvantagecaching.Cache;
import com.bitvantage.bitvantagecaching.CachingStore;
import com.bitvantage.bitvantagecaching.InMemoryHashStore;
import com.bitvantage.bitvantagecaching.LmdbStore;
import com.bitvantage.bitvantagecaching.RangedCachingStore;
import com.bitvantage.bitvantagecaching.RangedLmdbStore;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.UnboundedCache;
import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingRouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingStopDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingStopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingTripDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.TripDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.StopDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripDetails;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.DateKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.RouteIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.StopTimeKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripGroupKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripSequenceKey;
import com.publictransitanalytics.scoregenerator.distanceclient.types.DistanceCacheKey;
import com.publictransitanalytics.scoregenerator.distanceclient.types.WalkingDistanceMeasurement;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.StoredDistanceEstimator;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationDistanceKey;
import com.publictransitanalytics.scoregenerator.distanceclient.CachingDistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceFilter;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateRefiningReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.GoogleDistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.ManyDestinationsDistanceFilter;
import com.publictransitanalytics.scoregenerator.distanceclient.ManyOriginsDistanceFilter;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.output.SingleTimePointMap;
import com.publictransitanalytics.scoregenerator.output.SingleTimeSectorMap;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.tracking.ForwardMovingPath;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.tracking.RetrospectivePath;
import com.publictransitanalytics.scoregenerator.visitors.TransitRideVisitorFactory;
import com.publictransitanalytics.scoregenerator.visitors.VisitorFactory;
import com.publictransitanalytics.scoregenerator.visitors.WalkVisitorFactory;
import com.publictransitanalytics.scoregenerator.walking.BackwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripIdKey;
import com.publictransitanalytics.scoregenerator.output.TimeRangeSectorMap;
import com.publictransitanalytics.scoregenerator.publishing.LocalFilePublisher;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingEntryPoints;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoints;
import com.publictransitanalytics.scoregenerator.scoring.ReachedSectorScoreGenerator;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.concurrent.ForkJoinPool;
import org.opensextant.geodesy.Geodetic2DBounds;
import com.publictransitanalytics.scoregenerator.rider.RiderBehaviorFactory;

@Slf4j
/**
 * Parses inputs, builds up infrastructure, and delegates to solve reachability
 * problems.
 *
 * @author Public Transit Analytics
 */
public class Main {

    private static final Geodetic2DBounds SEATTLE_BOUNDS
            = new Geodetic2DBounds(
                    new Geodetic2DPoint(
                            new Longitude(-122.459696, Longitude.DEGREES),
                            new Latitude(47.734145, Latitude.DEGREES)),
                    new Geodetic2DPoint(
                            new Longitude(-122.224433, Longitude.DEGREES),
                            new Latitude(47.48172, Latitude.DEGREES)));

    private static final int NUM_LATITUDE_SECTORS = 100;
    private static final int NUM_LONGITUDE_SECTORS = 100;

    private static final double ESTIMATE_WALK_METERS_PER_SECOND = 1.1;

    private static final int MAX_DEPTH = 6;

    private static final String WALKING_DISTANCE_STORE
            = "new_walking_distance_store";
    private static final String CANDIDATE_STOP_DISTANCES_STORE
            = "candidate_distance_store";
    private static final String TRIP_SEQUENCE_STORE = "trip_sequence_store";
    private static final String STOP_TIMES_STORE = "stop_times_store";
    private static final String TRIPS_STORE = "trips_store";
    private static final String SERVICE_TYPES_STORE = "service_types_store";
    private static final String ROUTE_DETAILS_STORE = "route_details_store";
    private static final String TRIP_DETAILS_STORE = "trip_details_store";
    private static final String STOP_DETAILS_STORE = "stop_details_store";

    private static final String GTFS_DIRECTORY = "gtfs";

    private static final String STOP_TIMES_FILE = "stop_times.txt";
    private static final String FREQUENCIES_FILE = "frequencies.txt";
    private static final String CALENDAR_FILE = "calendar.txt";
    private static final String CALENDAR_DATES_FILE = "calendar_dates.txt";
    private static final String ROUTES_FILE = "routes.txt";
    private static final String TRIPS_FILE = "trips.txt";
    private static final String STOPS_FILE = "stops.txt";

    public static void main(String[] args) throws FileNotFoundException,
            IOException, ArgumentParserException, InterruptedException,
            ExecutionException {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser(
                "SeattleTransitIsochrone").defaultHelp(true)
                .description("Generate isochrone map data.");
        final MutuallyExclusiveGroup group = parser.addMutuallyExclusiveGroup();
        group.addArgument("-o", "--stopId");
        group.addArgument("-c", "--coordinate");
        parser.addArgument("-s", "--startTime");
        parser.addArgument("-e", "--endTime");
        parser.addArgument("-l", "--tripLengths").action(Arguments.append());
        parser.addArgument("-i", "--samplingInterval");
        parser.addArgument("-a", "--apiKey");
        parser.addArgument("-b", "--base");
        parser.addArgument("-r", "--revision");
        parser.addArgument("-n", "--investigate");
        parser.addArgument("-k", "--backward").action(Arguments.storeTrue());
        parser.addArgument("-p", "--points").action(Arguments.storeTrue());

        final Namespace ns = parser.parseArgs(args);
        final String startingStop = ns.get("stopId");

        final String coordinateString = ns.get("coordinate");
        final Geodetic2DPoint startingCoordinate = (coordinateString == null)
                ? null : new Geodetic2DPoint(coordinateString);

        final LocalDateTime startTime
                = LocalDateTime.parse(ns.get("startTime"));

        final String endTimeString = ns.get("endTime");

        final LocalDateTime endTime = (endTimeString != null) ? LocalDateTime.
                parse(endTimeString) : null;

        final List<String> durationMinuteStrings = ns.getList("tripLengths");

        final String basePathString = ns.get("base");
        final Path base = Paths.get(basePathString);

        final String key = ns.get("apiKey");

        final String revision = ns.get("revision");

        final Boolean backward = ns.getBoolean("backward");

        final Boolean pointMode = ns.getBoolean("points");

        final String samplingIntervalString = ns.get("samplingInterval");
        final Duration samplingInterval = (samplingIntervalString != null)
                ? Duration.ofMinutes(Long.valueOf(samplingIntervalString))
                : null;

        final Store<StopIdKey, StopDetails> stopDetailsBackingStore
                = new LmdbStore<>(base.resolve(revision).resolve(
                        STOP_DETAILS_STORE), StopDetails.class);
        final Cache<StopIdKey, StopDetails> stopDetailsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<StopIdKey, StopDetails> stopDetailsStore = new CachingStore(
                stopDetailsBackingStore, stopDetailsCache);

        final Reader stopDetailsReader = new FileReader(
                base.resolve(revision).resolve(GTFS_DIRECTORY).resolve(
                STOPS_FILE).toFile());

        final GTFSReadingStopDetailsDirectory stopDetailsDirectory
                = new GTFSReadingStopDetailsDirectory(stopDetailsStore,
                                                      stopDetailsReader);

        final NavigableSet<Duration> durations = new TreeSet<>();
        for (String durationMinuteString : durationMinuteStrings) {
            final Duration duration = Duration.ofMinutes(Integer.valueOf(
                    durationMinuteString));
            durations.add(duration);
        }

        final LocalDateTime earliestTime;
        if (backward) {
            earliestTime = startTime.minus(durations.last());
        } else {
            earliestTime = startTime;
        }

        final LocalDateTime latestTime;
        if (endTime != null) {
            if (backward) {
                latestTime = endTime;
            } else {
                latestTime = endTime.plus(durations.last());
            }
        } else if (backward) {
            latestTime = startTime;
        } else {
            latestTime = startTime.plus(durations.last());
        }

        final ImmutableBiMap<String, VisitableLocation> locationIdMap;
        final ImmutableBiMap<String, PointLocation> pointIdMap;
        final ImmutableBiMap<String, TransitStop> stopIdMap;

        SectorTable sectorTable = new SectorTable(
                SEATTLE_BOUNDS, NUM_LATITUDE_SECTORS, NUM_LONGITUDE_SECTORS);
        final ImmutableBiMap.Builder<String, VisitableLocation> locationMapBuilder
                = ImmutableBiMap.builder();
        final ImmutableBiMap.Builder<String, PointLocation> pointMapBuilder
                = ImmutableBiMap.builder();
        final ImmutableBiMap.Builder<String, TransitStop> stopMapBuilder
                = ImmutableBiMap.builder();

        for (final Sector sector : sectorTable.getSectors()) {
            locationMapBuilder.put(sector.getIdentifier(), sector);
        }

        for (final StopDetails stopDetails : stopDetailsDirectory
                .getAllStopDetails()) {
            final Geodetic2DPoint location = new Geodetic2DPoint(
                    new Longitude(stopDetails.getCoordinate().getLongitude(),
                                  Longitude.DEGREES),
                    new Latitude(stopDetails.getCoordinate().getLatitude(),
                                 Latitude.DEGREES));

            final Sector containingSector = sectorTable.findSector(location);
            if (containingSector != null) {
                final TransitStop stop = new TransitStop(
                        containingSector, stopDetails.getStopId(),
                        stopDetails.getStopName(), location);
                locationMapBuilder.put(stop.getIdentifier(), stop);
                pointMapBuilder.put(stop.getIdentifier(), stop);
                stopMapBuilder.put(stop.getIdentifier(), stop);
            } else {
                log.warn(String.format(
                        "Stop at %s location %s was skipped because it was not in the sector table.",
                        stopDetails, location));
            }
        }
        final PointLocation startLocation;

        if (startingCoordinate != null) {
            final Sector containingSector = sectorTable.findSector(
                    startingCoordinate);

            if (containingSector == null) {
                throw new SeattleIsochroneFatalException(String.format(
                        "Starting location %s was not in the SectorTable",
                        startingCoordinate));
            }

            final Landmark startingPoint = new Landmark(containingSector,
                                                        startingCoordinate);
            startLocation = startingPoint;
            locationMapBuilder.put(startingPoint.getIdentifier(),
                                   startingPoint);
            pointMapBuilder.put(startingPoint.getIdentifier(), startingPoint);
            locationIdMap = locationMapBuilder.build();
            pointIdMap = pointMapBuilder.build();
            stopIdMap = stopMapBuilder.build();
        } else {
            locationIdMap = locationMapBuilder.build();
            pointIdMap = pointMapBuilder.build();
            stopIdMap = stopMapBuilder.build();

            if (!pointIdMap.containsKey(startingStop)) {
                throw new SeattleIsochroneFatalException(String.format(
                        "Starting location %s is invalid.", startingStop));
            }
            startLocation = pointIdMap.get(startingStop);
        }

        final Store<TripGroupKey, TripDetails> tripDetailsBackingStore
                = new LmdbStore<>(base.resolve(revision).resolve(
                        TRIP_DETAILS_STORE), TripDetails.class);
        final Cache<TripGroupKey, TripDetails> tripDetailsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        Store<TripGroupKey, TripDetails> tripDetailsStore
                = new CachingStore(tripDetailsBackingStore, tripDetailsCache);

        final Reader tripReader = new FileReader(base.resolve(revision).resolve(
                GTFS_DIRECTORY).resolve(TRIPS_FILE).toFile());
        final TripDetailsDirectory tripDetailsDirectory
                = new GTFSReadingTripDetailsDirectory(tripDetailsStore,
                                                      tripReader);

        final RangedStore<TripSequenceKey, TripStop> tripSequenceBackingStore
                = new RangedLmdbStore<>(base.resolve(revision).resolve(
                        TRIP_SEQUENCE_STORE), TripStop.class);
        final Cache<TripSequenceKey, TripStop> tripSequenceCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = new RangedCachingStore<>(tripSequenceBackingStore,
                                           tripSequenceCache);

        final RangedStore<StopTimeKey, TripStop> stopTimesBackingStore
                = new RangedLmdbStore<>(
                        base.resolve(revision).resolve(STOP_TIMES_STORE),
                        TripStop.class);
        final Cache<StopTimeKey, TripStop> stopTimesCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = new RangedCachingStore<>(stopTimesBackingStore,
                                           stopTimesCache);

        final Store<TripIdKey, TripId> tripsBackingStore = new LmdbStore<>(
                base.resolve(revision).resolve(TRIPS_STORE),
                TripId.class);
        final Cache<TripIdKey, TripId> tripsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<TripIdKey, TripId> tripsStore
                = new CachingStore<>(tripsBackingStore, tripsCache);

        final Path frequenciesPath = base.resolve(revision).resolve(
                GTFS_DIRECTORY).resolve(FREQUENCIES_FILE);
        final Reader frequenciesReader;
        if (Files.exists(frequenciesPath)) {
            frequenciesReader = new FileReader(frequenciesPath.toFile());
        } else {
            frequenciesReader = new StringReader("");
        }
        final Reader stopTimesReader = new FileReader(base.resolve(revision).
                resolve(GTFS_DIRECTORY).resolve(STOP_TIMES_FILE).toFile());

        final GTFSReadingStopTimesDirectory stopTimesDirectory
                = new GTFSReadingStopTimesDirectory(
                        tripSequenceStore, stopTimesStore, tripsStore,
                        frequenciesReader, stopTimesReader);

        final Store<DateKey, ServiceSet> serviceTypesBackingStore
                = new LmdbStore<>(
                        base.resolve(revision).resolve(SERVICE_TYPES_STORE),
                        ServiceSet.class);

        final Cache<DateKey, ServiceSet> serviceTypesCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<DateKey, ServiceSet> serviceTypesStore
                = new CachingStore<>(serviceTypesBackingStore,
                                     serviceTypesCache);

        final Reader calendarReader = new FileReader(
                base.resolve(revision).resolve(GTFS_DIRECTORY).
                resolve(CALENDAR_FILE).toFile());
        final Reader calendarDatesReader = new FileReader(base.
                resolve(revision).resolve(GTFS_DIRECTORY).
                resolve(CALENDAR_DATES_FILE).toFile());

        final GTFSReadingServiceTypeCalendar serviceTypeCalendar
                = new GTFSReadingServiceTypeCalendar(
                        serviceTypesStore, calendarReader, calendarDatesReader);

        final Store<RouteIdKey, RouteDetails> routeDetailsBackingStore
                = new LmdbStore<>(
                        base.resolve(revision).resolve(ROUTE_DETAILS_STORE),
                        RouteDetails.class);
        final Cache<RouteIdKey, RouteDetails> routeDetailsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<RouteIdKey, RouteDetails> routeDetailsStore
                = new CachingStore(routeDetailsBackingStore,
                                   routeDetailsCache);
        final Reader routeReader = new FileReader(base.resolve(revision)
                .resolve(GTFS_DIRECTORY).resolve(ROUTES_FILE).toFile());

        final RouteDetailsDirectory routeDetailsDirectory
                = new GTFSReadingRouteDetailsDirectory(
                        routeDetailsStore, routeReader);

        final EntryPoints entryPoints = new DirectoryReadingEntryPoints(
                earliestTime, latestTime, stopTimesDirectory, 
                routeDetailsDirectory, tripDetailsDirectory, 
                serviceTypeCalendar, stopIdMap);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceBackingStore
                = new LmdbStore(base.resolve(WALKING_DISTANCE_STORE),
                                WalkingDistanceMeasurement.class);
        final Cache<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceMemoryCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceCacheStore
                = new CachingStore<>(walkingDistanceBackingStore,
                                     walkingDistanceMemoryCache);
        final Cache<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceCache
                = new UnboundedCache<>(walkingDistanceCacheStore);

        final DistanceClient distanceClient = new CachingDistanceClient(
                walkingDistanceCache, new GoogleDistanceClient(key));

        final Path candidateDistancesStorePath = base.resolve(revision)
                .resolve(CANDIDATE_STOP_DISTANCES_STORE);
        final RangedStore<LocationDistanceKey, String> candidateStopDistancesBackingStore
                = new RangedLmdbStore<>(candidateDistancesStorePath,
                                        String.class);
        final Cache<LocationDistanceKey, String> candidateStopDistancesCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final RangedStore<LocationDistanceKey, String> candidateDistancesStore
                = new RangedCachingStore<>(
                        candidateStopDistancesBackingStore,
                        candidateStopDistancesCache);

        final ImmutableBiMap<String, PointLocation> pointMap
                = pointMapBuilder.build();

        final double maxWalkingDistance
                = ESTIMATE_WALK_METERS_PER_SECOND * durations.last()
                .getSeconds();

        final DistanceEstimator distanceEstimator
                = new StoredDistanceEstimator(
                        sectorTable.getSectors(), pointMap.values(),
                        maxWalkingDistance, candidateDistancesStore);

        final RiderBehaviorFactory riderFactory;
        final TimeTracker timeTracker;
        final DistanceFilter distanceFilter;
        final MovementPath basePath;
        if (backward == null || !backward) {
            riderFactory = new ForwardRiderBehaviorFactory(entryPoints);
            timeTracker = new ForwardTimeTracker();
            distanceFilter = new ManyDestinationsDistanceFilter(
                    distanceClient);
            basePath = new ForwardMovingPath(ImmutableList.of());
        } else {
            riderFactory = new RetrospectiveRiderBehaviorFactory(entryPoints);
            timeTracker = new BackwardTimeTracker();
            distanceFilter = new ManyOriginsDistanceFilter(distanceClient);
            basePath = new RetrospectivePath(ImmutableList.of());
        }

        final ReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        distanceFilter, distanceEstimator, timeTracker,
                        ESTIMATE_WALK_METERS_PER_SECOND, locationIdMap);

        final Set<VisitorFactory> visitorFactories = ImmutableSet.of(
                new TransitRideVisitorFactory(MAX_DEPTH, riderFactory),
                new WalkVisitorFactory(
                        MAX_DEPTH, reachabilityClient, timeTracker));

        final LocalFilePublisher publisher = new LocalFilePublisher();
        final ReachedSectorScoreGenerator scoreGenerator
                = new ReachedSectorScoreGenerator();

        try {
            final Workflow workflow
                    = new Workflow(timeTracker, basePath, visitorFactories);

            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final String output;

            if (pointMode == null || !pointMode) {
                if (endTime == null && durations.size() == 1) {
                    workflow.getPathsAtTime(
                            durations.first(), startTime, startLocation);
                    final int score = scoreGenerator.getScore(
                            sectorTable, startTime);
                    final SingleTimeSectorMap map = new SingleTimeSectorMap(
                            sectorTable, startLocation, startTime,
                            durations.first(), score);

                    output = gson.toJson(map);
                    publisher.publish(output);
                } else if (durations.size() == 1) {
                    workflow.getPathsOverRange(
                            startLocation, durations.first(), startTime,
                            endTime, samplingInterval);
                    final int score = scoreGenerator.getScore(
                            sectorTable, startTime, endTime, samplingInterval);
                    final TimeRangeSectorMap map = new TimeRangeSectorMap(
                            sectorTable, startLocation, startTime, endTime,
                            samplingInterval, durations.first(), score, 10);

                    output = gson.toJson(map);
                    publisher.publish(output);
                } else {
                    throw new UnsupportedOperationException(
                            "No problem to solve.");
                }
            } else if (endTime == null && durations.size() == 1) {
                workflow.getPathsAtTime(
                        durations.first(), startTime, startLocation);
                final SingleTimePointMap map = new SingleTimePointMap(
                        sectorTable, pointIdMap.values(), startLocation,
                        startTime, durations.first());
                output = gson.toJson(map);
                publisher.publish(output);
            } else if (durations.size() == 1) {
                throw new UnsupportedOperationException("No problem to solve.");
            } else if (durations.size() > 1) {
                throw new UnsupportedOperationException("No problem to solve.");
            } else {
                throw new UnsupportedOperationException("No problem to solve.");
            }

        } finally {
            ForkJoinPool.commonPool().shutdown();
        }
    }
}
