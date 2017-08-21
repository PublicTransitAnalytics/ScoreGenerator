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

import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import com.bitvantage.bitvantagecaching.Cache;
import com.bitvantage.bitvantagecaching.CachingStore;
import com.bitvantage.bitvantagecaching.InMemoryHashStore;
import com.bitvantage.bitvantagecaching.LmdbStore;
import com.bitvantage.bitvantagecaching.CachingRangedStore;
import com.bitvantage.bitvantagecaching.InMemoryTreeStore;
import com.bitvantage.bitvantagecaching.RangedCache;
import com.bitvantage.bitvantagecaching.RangedLmdbStore;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.ReaderControlledRangedStore;
import com.bitvantage.bitvantagecaching.ReaderControlledStore;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.UnboundedCache;
import com.bitvantage.bitvantagecaching.UnboundedRangedCache;
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
import com.publictransitanalytics.scoregenerator.distanceclient.ManyDestinationsDistanceFilter;
import com.publictransitanalytics.scoregenerator.distanceclient.ManyOriginsDistanceFilter;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.output.QualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.walking.BackwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import com.google.common.collect.ImmutableBiMap;
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
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationKey;
import com.publictransitanalytics.scoregenerator.distanceclient.GraphhopperLocalDistanceClient;
import com.publictransitanalytics.scoregenerator.geography.EndpointDeterminer;
import com.publictransitanalytics.scoregenerator.geography.NearestPointEndpointDeterminer;
import com.publictransitanalytics.scoregenerator.geography.GeoJsonWaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetectorException;
import com.publictransitanalytics.scoregenerator.output.MapGenerator;
import com.publictransitanalytics.scoregenerator.output.NetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.PointAccessibility;
import com.publictransitanalytics.scoregenerator.publishing.LocalFilePublisher;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingEntryPoints;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoints;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.concurrent.ForkJoinPool;
import org.opensextant.geodesy.Geodetic2DBounds;
import com.publictransitanalytics.scoregenerator.rider.RiderBehaviorFactory;
import com.publictransitanalytics.scoregenerator.scoring.CountScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRangeWorkflow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import com.publictransitanalytics.scoregenerator.workflow.Workflow;

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

    private static final double ESTIMATE_WALK_METERS_PER_SECOND = 2.0;

    private static final int MAX_DEPTH = 6;

    private static final int NUM_DISPLAY_BUCKETS = 9;
    private static final int NUM_PROBABILITY_BUCKETS = 40;
    private static final int DISPLAY_BUCKET_SCALE = 5;

    private static final String WALKING_DISTANCE_STORE
            = "new_walking_distance_store";
    private static final String MAX_CANDIDATE_STOP_DISTANCE_STORE
            = "max_candidate_distance_store";
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

    private static final String WATER_BODIES_FILE = "water.json";

    private static final String SEATTLE_OSM_FILE = "washington-latest.osm.pbf";
    private static final String GRAPHHOPPER_DIRECTORY = "graphhopper_files";

    public static void main(String[] args) throws FileNotFoundException,
            IOException, ArgumentParserException, InterruptedException,
            ExecutionException, WaterDetectorException {

        final ArgumentParser parser = ArgumentParsers.newArgumentParser(
                "ScoreGenerator").defaultHelp(true)
                .description("Generate isochrone map data.");

        parser.addArgument("-l", "--tripLengths").action(Arguments.append());
        parser.addArgument("-i", "--samplingInterval");
        parser.addArgument("-d", "--baseDirectory");
        parser.addArgument("-k", "--backward").action(Arguments.storeTrue());
        parser.addArgument("-p", "--poolSize");
        parser.addArgument("-t", "--taskSize");

        final Subparsers subparsers = parser.addSubparsers().dest("command");

        final Subparser generateNetworkAccessibilityParser = subparsers
                .addParser(
                        "generateNetworkAccessibility");
        generateNetworkAccessibilityParser.addArgument("-a", "--startTime");
        generateNetworkAccessibilityParser.addArgument("-s", "--span");
        generateNetworkAccessibilityParser.addArgument("-f", "--files");

        final Subparser generateSampledNetworkAccessibilityParser
                = subparsers.addParser("generateSampledNetworkAccessibility");
        generateSampledNetworkAccessibilityParser.addArgument("-a",
                                                              "--startTime");
        generateSampledNetworkAccessibilityParser.addArgument("-s", "--span");
        generateSampledNetworkAccessibilityParser.addArgument("-f", "--files");
        generateSampledNetworkAccessibilityParser.addArgument("-m", "--samples");

        final Subparser generatePointAccessibilityParser = subparsers.addParser(
                "generatePointAccessibility");
        generatePointAccessibilityParser.addArgument("-a", "--startTime");
        generatePointAccessibilityParser.addArgument("-s", "--span");
        generatePointAccessibilityParser.addArgument("-f", "--files");
        generatePointAccessibilityParser.addArgument("-c", "--coordinate");

        final Namespace namespace = parser.parseArgs(args);

        final List<String> durationMinuteStrings
                = namespace.getList("tripLengths");
        final NavigableSet<Duration> durations = new TreeSet<>();
        for (String durationMinuteString : durationMinuteStrings) {
            final Duration duration = Duration.ofMinutes(Integer.valueOf(
                    durationMinuteString));
            durations.add(duration);
        }

        final String baseDirectoryString = namespace.get("baseDirectory");
        final Path root = Paths.get(baseDirectoryString);

        final String key = namespace.get("apiKey");

        final Boolean backwardObject = namespace.getBoolean("backward");
        final boolean backward
                = (backwardObject == null) ? false : backwardObject;

        final String samplingIntervalString = namespace.get(
                "samplingInterval");
        final Duration samplingInterval = (samplingIntervalString != null)
                ? Duration.ofMinutes(Long.valueOf(samplingIntervalString))
                : null;

        final String command = namespace.get("command");

        final int poolSize = Integer.valueOf(namespace.get("poolSize"));

        final int taskSize = Integer.valueOf(namespace.get("taskSize"));

        final ReachedSectorScoreGenerator scoreGenerator
                = new ReachedSectorScoreGenerator();
        final Gson serializer = new GsonBuilder().setPrettyPrinting().create();
        final LocalFilePublisher publisher = new LocalFilePublisher();

        final Path waterFilePath = root.resolve(WATER_BODIES_FILE);
        final WaterDetector waterDetector
                = new GeoJsonWaterDetector(waterFilePath);

        final NearestPointEndpointDeterminer endpointDeterminer
                = new NearestPointEndpointDeterminer();
        final DistanceClient distanceClient
                = buildDistanceClient(root, key, endpointDeterminer,
                                      waterDetector);

        final ForkJoinPool pool = new ForkJoinPool(poolSize);

        final SectorTable sectorTable = generateSectors(waterDetector);

        final MapGenerator mapGenerator = new MapGenerator();

        try {
            if ("generatePointAccessibility".equals(command)) {
                final PathScoreCard scoreCard = new PathScoreCard();

                generatePointAccessibility(
                        namespace, root, scoreCard, sectorTable, backward,
                        samplingInterval, durations, distanceClient,
                        endpointDeterminer, pool, taskSize, publisher,
                        serializer, mapGenerator);
            } else if ("generateNetworkAccessibility".equals(command)) {
                final ScoreCard scoreCard = new CountScoreCard();

                generateNetworkAccessibility(
                        namespace, root, scoreCard, sectorTable, backward,
                        samplingInterval, durations, waterDetector,
                        distanceClient, endpointDeterminer, pool, taskSize,
                        publisher, serializer, mapGenerator);
            } else if ("generateSampledNetworkAccessibility".equals(command)) {
                final ScoreCard scoreCard = new CountScoreCard();

                generateSampledNetworkAccessibility(
                        namespace, root, scoreCard, sectorTable, backward,
                        samplingInterval, durations, waterDetector,
                        distanceClient, endpointDeterminer, pool, taskSize,
                        publisher, serializer, mapGenerator);
            }
        } finally {

        }

    }

    private static LocalDateTime getEarliestTime(
            final LocalDateTime startTime, final Duration maxDuration,
            final boolean backward) {
        final LocalDateTime earliestTime;
        if (backward) {
            earliestTime = startTime.minus(maxDuration);
        } else {
            earliestTime = startTime;
        }
        return earliestTime;
    }

    private static LocalDateTime getLatestTime(
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration maxDuration, final boolean backward) {
        final LocalDateTime latestTime;
        if (endTime != null) {
            if (backward) {
                latestTime = endTime;
            } else {
                latestTime = endTime.plus(maxDuration);
            }
        } else if (backward) {
            latestTime = startTime;
        } else {
            latestTime = startTime.plus(maxDuration);
        }
        return latestTime;
    }

    private static EntryPoints buildEntryPoints(
            final Path root, final String files,
            final LocalDateTime earliestTime, final LocalDateTime latestTime,
            final ImmutableBiMap<String, TransitStop> stopIdMap)
            throws IOException, InterruptedException {

        final ServiceTypeCalendar serviceTypeCalendar
                = buildServiceTypeCalendar(root, files);
        final TripDetailsDirectory tripDetailsDirectory
                = buildTripDetailsDirectory(root, files);
        final RouteDetailsDirectory routeDetailsDirectory
                = buildRouteDetailsDirectory(root, files);
        final StopTimesDirectory stopTimesDirectory
                = buildStopTimesDirectory(root, files);

        final EntryPoints entryPoints = buildEntryPoints(
                earliestTime, latestTime, serviceTypeCalendar,
                tripDetailsDirectory, routeDetailsDirectory, stopTimesDirectory,
                stopIdMap);

        return entryPoints;
    }

    private static void generateNetworkAccessibility(
            final Namespace namespace, final Path root,
            final ScoreCard scoreCard, final SectorTable sectorTable,
            final boolean backward, final Duration samplingInterval,
            final NavigableSet<Duration> durations,
            final WaterDetector waterDetector,
            final DistanceClient distanceClient,
            final NearestPointEndpointDeterminer endpointDeterminer,
            final ForkJoinPool pool, final int taskSize,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator)
            throws IOException, InterruptedException, ExecutionException {
        final String files = namespace.get("files");
        final LocalDateTime startTime
                = LocalDateTime.parse(namespace.get("startTime"));

        final Duration span = Duration.parse(namespace.get("span"));

        final LocalDateTime endTime = startTime.plus(span);

        final Set<Sector> measuredSectors = sectorTable.getSectors();
        final Set<PointLocation> centerPoints = measuredSectors.stream()
                .map(sector -> new Landmark(
                sector, sector.getCanonicalPoint()))
                .collect(Collectors.toSet());

        final Set<TaskIdentifier> tasks = getTasks(
                centerPoints, startTime, endTime, samplingInterval);
        try {
            executeTasks(tasks, root, files, scoreCard, sectorTable,
                         centerPoints, startTime, endTime, backward,
                         distanceClient, endpointDeterminer, durations.last(),
                         pool, taskSize);

            final NetworkAccessibility map = new NetworkAccessibility(
                    tasks, scoreCard, sectorTable, centerPoints, startTime,
                    endTime, durations.last(), samplingInterval, backward);
            publisher.publish(serializer.toJson(map));
            mapGenerator.makeMap(sectorTable, scoreCard);
        } finally {
            distanceClient.close();
        }
    }

    private static Set<TaskIdentifier> getTasks(
            final Set<PointLocation> centerPoints,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration samplingInterval) {
        final ImmutableSet.Builder<TaskIdentifier> tasksBuilder
                = ImmutableSet.builder();

        for (final PointLocation centerPoint : centerPoints) {
            LocalDateTime time = startTime;
            while (time.isBefore(endTime)) {
                final TaskIdentifier task = new TaskIdentifier(
                        time, centerPoint, "generatePointAccessibility");
                tasksBuilder.add(task);
                time = time.plus(samplingInterval);
            }
        }
        return tasksBuilder.build();
    }

    private static void executeTasks(
            final Set<TaskIdentifier> tasks, final Path root,
            final String files, final ScoreCard scoreCard,
            final SectorTable sectorTable,
            final Set<PointLocation> centerPoints,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final boolean backward, final DistanceClient distanceClient,
            final EndpointDeterminer endpointDeterminer,
            final Duration longestDuration,
            final ForkJoinPool pool, final int taskSize)
            throws InterruptedException, IOException, ExecutionException {
        final StopDetailsDirectory stopDetailsDirectory
                = buildStopDetailsDirectory(root, files);

        final ImmutableBiMap<String, TransitStop> stopIdMap
                = buildTransitStopIdMap(sectorTable, stopDetailsDirectory);

        final ImmutableBiMap<String, PointLocation> pointIdMap
                = buildPointIdMap(centerPoints, stopIdMap);
        final ImmutableBiMap<String, VisitableLocation> locationIdMap
                = buildLocationIdMap(pointIdMap, sectorTable);

        final LocalDateTime earliestTime = getEarliestTime(
                startTime, longestDuration, backward);
        final LocalDateTime latestTime = getLatestTime(
                startTime, endTime, longestDuration, backward);

        final EntryPoints entryPoints = buildEntryPoints(
                root, files, earliestTime, latestTime, stopIdMap);

        final RiderBehaviorFactory riderFactory;
        final TimeTracker timeTracker;
        final DistanceFilter distanceFilter;
        if (!backward) {
            riderFactory = new ForwardRiderBehaviorFactory(entryPoints);
            timeTracker = new ForwardTimeTracker();
            distanceFilter = new ManyDestinationsDistanceFilter(
                    distanceClient);
        } else {
            riderFactory = new RetrospectiveRiderBehaviorFactory(
                    entryPoints);
            timeTracker = new BackwardTimeTracker();
            distanceFilter = new ManyOriginsDistanceFilter(
                    distanceClient);
        }

        final DistanceEstimator distanceEstimator
                = buildDistanceEstimator(
                        sectorTable, endpointDeterminer, centerPoints,
                        pointIdMap, longestDuration, root, files);

        try {
            final ReachabilityClient reachabilityClient
                    = buildReachabilityClient(distanceFilter, distanceEstimator,
                                              timeTracker, locationIdMap);
            final Workflow workflow
                    = new DynamicProgrammingRangeWorkflow(
                            MAX_DEPTH, scoreCard, sectorTable, timeTracker,
                            pool, taskSize);

            workflow.getPathsForTasks(
                    longestDuration,
                    ImmutableSet.of(ModeType.TRANSIT, ModeType.WALKING),
                    riderFactory, reachabilityClient, tasks);
        } finally {
            distanceEstimator.close();
        }

    }

    private static void generateSampledNetworkAccessibility(
            final Namespace namespace, final Path root,
            final ScoreCard scoreCard, final SectorTable sectorTable,
            final boolean backward, final Duration samplingInterval,
            final NavigableSet<Duration> durations,
            final WaterDetector waterDetector,
            final DistanceClient distanceClient,
            final NearestPointEndpointDeterminer endpointDeterminer,
            final ForkJoinPool pool, final int taskSize,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator)
            throws IOException, InterruptedException, ExecutionException {

        final String files = namespace.get("files");
        final LocalDateTime startTime
                = LocalDateTime.parse(namespace.get("startTime"));

        final Duration span = Duration.parse(namespace.get("span"));

        final int samples = Integer.valueOf(namespace.get("samples"));

        final LocalDateTime endTime = startTime.plus(span);

        final List<Sector> sectorList
                = new ArrayList(sectorTable.getSectors());
        Collections.shuffle(sectorList);
        final ImmutableSet<Sector> measuredSectors
                = ImmutableSet.copyOf(sectorList.subList(0, samples));
        final Set<PointLocation> centerPoints = measuredSectors.stream()
                .map(sector -> new Landmark(
                sector, sector.getCanonicalPoint()))
                .collect(Collectors.toSet());

        final Set<TaskIdentifier> tasks = getTasks(
                centerPoints, startTime, endTime, samplingInterval);

        try {
            executeTasks(tasks, root, files, scoreCard, sectorTable,
                         centerPoints, startTime, endTime, backward,
                         distanceClient, endpointDeterminer, durations.last(),
                         pool, taskSize);

            final NetworkAccessibility map
                    = new NetworkAccessibility(
                            tasks, scoreCard, sectorTable, centerPoints,
                            startTime, endTime, durations.last(),
                            samplingInterval, backward);
            publisher.publish(serializer.toJson(map));
            mapGenerator.makeMap(sectorTable, scoreCard);
        } finally {
            distanceClient.close();
        }

    }

    private static void generatePointAccessibility(
            final Namespace namespace, final Path root,
            final PathScoreCard scoreCard, final SectorTable sectorTable,
            final boolean backward, final Duration samplingInterval,
            final NavigableSet<Duration> durations,
            final DistanceClient distanceClient,
            final NearestPointEndpointDeterminer endpointDeterminer,
            final ForkJoinPool pool, final int taskSize,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator)
            throws IOException, InterruptedException, ExecutionException {

        final String coordinateString = namespace.get("coordinate");
        final Geodetic2DPoint centerCoordinate = (coordinateString == null)
                ? null : new Geodetic2DPoint(coordinateString);
        final Landmark centerPoint = buildCenterPoint(
                sectorTable, centerCoordinate);

        final String files = namespace.get("files");
        final LocalDateTime startTime
                = LocalDateTime.parse(namespace.get("startTime"));

        final String spanString = namespace.get("span");

        final Duration span = (spanString == null) ? null : Duration.parse(
                spanString);

        final LocalDateTime endTime
                = (span == null) ? null : startTime.plus(span);

        final String experimentName = "TQPA";
        final Set<TaskIdentifier> tasks;
        LocalDateTime lastTime;
        if (endTime == null && durations.size() == 1) {
            final TaskIdentifier task = new TaskIdentifier(
                    startTime, centerPoint, experimentName);
            tasks = Collections.singleton(task);
            lastTime = startTime;
        } else if (durations.size() == 1) {
            final ImmutableSet.Builder<TaskIdentifier> tasksBuilder
                    = ImmutableSet.builder();
            LocalDateTime time = startTime;
            lastTime = startTime;
            while (time.isBefore(endTime)) {
                tasksBuilder.add(new TaskIdentifier(
                        time, centerPoint, experimentName));
                lastTime = time;
                time = time.plus(samplingInterval);
            }
            tasks = tasksBuilder.build();
        } else {
            throw new UnsupportedOperationException(
                    "Cannot handle request.");
        }

        try {
            executeTasks(tasks, root, files, scoreCard, sectorTable,
                         Collections.singleton(centerPoint), startTime, endTime,
                         backward, distanceClient, endpointDeterminer,
                         durations.last(), pool, taskSize);

            if (tasks.size() == 1) {
                final QualifiedPointAccessibility map
                        = new QualifiedPointAccessibility(
                                tasks.iterator().next(), scoreCard, sectorTable,
                                durations.first(), backward);
                final String output = serializer.toJson(map);
                publisher.publish(output);
            } else {
                final PointAccessibility map = new PointAccessibility(
                        tasks, scoreCard, sectorTable, centerPoint, startTime,
                        lastTime, samplingInterval, durations.first(),
                        backward);
                final String output = serializer.toJson(map);
                publisher.publish(output);
                mapGenerator.makeMap(sectorTable, scoreCard);
            }

        } finally {
            distanceClient.close();
        }
    }

    private static SectorTable generateSectors(
            final WaterDetector waterDetector) {
        final SectorTable sectorTable = new SectorTable(
                SEATTLE_BOUNDS, NUM_LATITUDE_SECTORS, NUM_LONGITUDE_SECTORS,
                waterDetector);
        return sectorTable;
    }

    private static ImmutableBiMap<String, TransitStop> buildTransitStopIdMap(
            final SectorTable sectorTable,
            final StopDetailsDirectory stopDetailsDirectory)
            throws InterruptedException {
        final ImmutableBiMap.Builder<String, TransitStop> stopMapBuilder
                = ImmutableBiMap.builder();
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
                stopMapBuilder.put(stop.getIdentifier(), stop);
            } else {
                log.info(
                        "Stop at {} location {} was skipped because it was not in the sector table.",
                        stopDetails, location);
            }
        }
        return stopMapBuilder.build();
    }

    private static Landmark buildCenterPoint(
            final SectorTable sectorTable,
            final Geodetic2DPoint centerCoordinate) {
        final Sector containingSector = sectorTable.findSector(
                centerCoordinate);

        if (containingSector == null) {
            throw new ScoreGeneratorFatalException(String.format(
                    "Starting location %s was not in the SectorTable",
                    centerCoordinate));
        }
        final Landmark centerPoint = new Landmark(containingSector,
                                                  centerCoordinate);
        return centerPoint;
    }

    private static ImmutableBiMap<String, PointLocation> buildPointIdMap(
            final Set<PointLocation> centerPoints,
            final ImmutableBiMap<String, TransitStop> transitStops) {
        final ImmutableBiMap.Builder<String, PointLocation> pointMapBuilder
                = ImmutableBiMap.builder();
        pointMapBuilder.putAll(transitStops);

        for (final PointLocation centerPoint : centerPoints) {
            pointMapBuilder.put(centerPoint.getIdentifier(), centerPoint);
        }
        final ImmutableBiMap<String, PointLocation> pointIdMap
                = pointMapBuilder.build();
        return pointIdMap;
    }

    private static ImmutableBiMap<String, VisitableLocation> buildLocationIdMap(
            final ImmutableBiMap<String, PointLocation> pointIdMap,
            final SectorTable sectorTable) {
        final ImmutableBiMap.Builder<String, VisitableLocation> locationMapBuilder
                = ImmutableBiMap.builder();
        locationMapBuilder.putAll(pointIdMap);
        for (final Sector sector : sectorTable.getSectors()) {
            locationMapBuilder.put(sector.getIdentifier(), sector);
        }
        return locationMapBuilder.build();
    }

    private static StopDetailsDirectory buildStopDetailsDirectory(
            final Path baseDirectory, final String revision)
            throws InterruptedException, IOException {
        final Store<StopIdKey, StopDetails> stopDetailsBackingStore
                = new LmdbStore<>(baseDirectory.resolve(revision).resolve(
                        STOP_DETAILS_STORE), StopDetails.class
                );
        final Cache<StopIdKey, StopDetails> stopDetailsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<StopIdKey, StopDetails> stopDetailsStore = new CachingStore(
                stopDetailsBackingStore, stopDetailsCache);

        final Reader stopDetailsReader = new FileReader(
                baseDirectory.resolve(revision).resolve(GTFS_DIRECTORY).resolve(
                        STOPS_FILE).toFile());

        final GTFSReadingStopDetailsDirectory stopDetailsDirectory
                = new GTFSReadingStopDetailsDirectory(stopDetailsStore,
                                                      stopDetailsReader);
        return stopDetailsDirectory;
    }

    private static DistanceClient buildDistanceClient(
            final Path baseDirectory, final String key,
            final NearestPointEndpointDeterminer endpointDeterminer,
            final WaterDetector waterDetector) {
        final Store<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceBackingStore
                = new LmdbStore(baseDirectory.resolve(WALKING_DISTANCE_STORE),
                                WalkingDistanceMeasurement.class
                );
        final Store<DistanceCacheKey, WalkingDistanceMeasurement> limitedAccessWalkingDistanceStore
                = new ReaderControlledStore(walkingDistanceBackingStore, 126);
        final Cache<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceMemoryCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceCacheStore
                = new CachingStore<>(limitedAccessWalkingDistanceStore,
                                     walkingDistanceMemoryCache);
        final Cache<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceCache
                = new UnboundedCache<>(walkingDistanceCacheStore);

        final GraphhopperLocalDistanceClient graphhopperDistanceClient
                = new GraphhopperLocalDistanceClient(
                        baseDirectory.resolve(SEATTLE_OSM_FILE),
                        baseDirectory.resolve(GRAPHHOPPER_DIRECTORY),
                        endpointDeterminer);
        final DistanceClient distanceClient = new CachingDistanceClient(
                walkingDistanceCache, graphhopperDistanceClient);

        return distanceClient;
    }

    private static ReachabilityClient buildReachabilityClient(
            final DistanceFilter distanceFilter,
            final DistanceEstimator distanceEstimator,
            final TimeTracker timeTracker,
            final ImmutableBiMap<String, VisitableLocation> locationIdMap) {
        final ReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        distanceFilter, distanceEstimator, timeTracker,
                        ESTIMATE_WALK_METERS_PER_SECOND, locationIdMap);
        return reachabilityClient;
    }

    private static DistanceEstimator buildDistanceEstimator(
            final SectorTable sectorTable,
            final EndpointDeterminer endpointDeterminer,
            final Set<PointLocation> centerPoints,
            final ImmutableBiMap<String, PointLocation> pointIdMap,
            final Duration maxDuration, final Path baseDirectory,
            final String revision) throws InterruptedException {
        final Path candidateDistancesStorePath = baseDirectory.resolve(revision)
                .resolve(CANDIDATE_STOP_DISTANCES_STORE);
        final RangedStore<LocationDistanceKey, String> candidateStopDistancesBackingStore
                = new RangedLmdbStore<>(candidateDistancesStorePath,
                                        new LocationDistanceKey.Materializer(),
                                        String.class);
        final ReaderControlledRangedStore<LocationDistanceKey, String> limitedAccessCandidateStopDistancesStore
                = new ReaderControlledRangedStore(
                        candidateStopDistancesBackingStore, 126);
        final RangedCache<LocationDistanceKey, String> candidateStopDistancesCache
                = new UnboundedRangedCache<>(new InMemoryTreeStore<>());
        final RangedStore<LocationDistanceKey, String> candidateDistancesStore
                = new CachingRangedStore<>(
                        limitedAccessCandidateStopDistancesStore,
                        candidateStopDistancesCache);

        final Path maxCandidateDistanceStorePath = baseDirectory.resolve(
                revision).resolve(MAX_CANDIDATE_STOP_DISTANCE_STORE);
        final Store<LocationKey, Double> maxCandidateDistancesBackingStore
                = new LmdbStore<>(maxCandidateDistanceStorePath, Double.class
                );
        final Cache<LocationKey, Double> maxCandidateDistanceCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<LocationKey, Double> maxCandidateDistanceStore
                = new CachingStore<>(maxCandidateDistancesBackingStore,
                                     maxCandidateDistanceCache);

        final double maxWalkingDistance
                = ESTIMATE_WALK_METERS_PER_SECOND * maxDuration.getSeconds();

        final DistanceEstimator distanceEstimator = new StoredDistanceEstimator(
                centerPoints, sectorTable.getSectors(), pointIdMap.values(),
                maxWalkingDistance, maxCandidateDistanceStore,
                candidateDistancesStore, endpointDeterminer);
        return distanceEstimator;
    }

    private static ServiceTypeCalendar buildServiceTypeCalendar(
            final Path root, final String revision)
            throws IOException, InterruptedException {
        final Store<DateKey, ServiceSet> serviceTypesBackingStore
                = new LmdbStore<>(root.resolve(revision).resolve(
                        SERVICE_TYPES_STORE), ServiceSet.class);

        final Cache<DateKey, ServiceSet> serviceTypesCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<DateKey, ServiceSet> serviceTypesStore
                = new CachingStore<>(serviceTypesBackingStore,
                                     serviceTypesCache);

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
            final Path root, final String revision)
            throws IOException, InterruptedException {
        final Store<TripGroupKey, TripDetails> tripDetailsBackingStore
                = new LmdbStore<>(root.resolve(revision).resolve(
                        TRIP_DETAILS_STORE), TripDetails.class
                );
        final Cache<TripGroupKey, TripDetails> tripDetailsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        Store<TripGroupKey, TripDetails> tripDetailsStore
                = new CachingStore(tripDetailsBackingStore, tripDetailsCache);

        final Reader tripReader = new FileReader(root.resolve(revision)
                .resolve(GTFS_DIRECTORY).resolve(TRIPS_FILE).toFile());
        final TripDetailsDirectory tripDetailsDirectory
                = new GTFSReadingTripDetailsDirectory(tripDetailsStore,
                                                      tripReader);
        return tripDetailsDirectory;
    }

    private static StopTimesDirectory buildStopTimesDirectory(
            final Path root, final String revision)
            throws IOException, InterruptedException {
        final Path tripSequenceStorePath
                = root.resolve(revision).resolve(TRIP_SEQUENCE_STORE);
        final RangedStore<TripSequenceKey, TripStop> tripSequenceBackingStore
                = new RangedLmdbStore<>(tripSequenceStorePath,
                                        new TripSequenceKey.Materializer(),
                                        TripStop.class
                );
        final RangedCache<TripSequenceKey, TripStop> tripSequenceCache
                = new UnboundedRangedCache<>(new InMemoryTreeStore<>());
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = new CachingRangedStore<>(tripSequenceBackingStore,
                                           tripSequenceCache);

        final Path stopTimesStorePath = root.resolve(revision)
                .resolve(STOP_TIMES_STORE);
        final RangedStore<StopTimeKey, TripStop> stopTimesBackingStore
                = new RangedLmdbStore<>(stopTimesStorePath,
                                        new StopTimeKey.Materializer(),
                                        TripStop.class
                );
        final RangedCache<StopTimeKey, TripStop> stopTimesCache
                = new UnboundedRangedCache<>(new InMemoryTreeStore<>());
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = new CachingRangedStore<>(stopTimesBackingStore,
                                           stopTimesCache);

        final Store<TripIdKey, TripId> tripsBackingStore = new LmdbStore<>(
                root.resolve(revision).resolve(TRIPS_STORE), TripId.class
        );
        final Cache<TripIdKey, TripId> tripsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<TripIdKey, TripId> tripsStore
                = new CachingStore<>(tripsBackingStore, tripsCache);

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
                        tripSequenceStore, stopTimesStore, tripsStore,
                        frequenciesReader, stopTimesReader);
        return stopTimesDirectory;
    }

    private static RouteDetailsDirectory buildRouteDetailsDirectory(
            final Path root, final String revision)
            throws IOException, InterruptedException {
        final Store<RouteIdKey, RouteDetails> routeDetailsBackingStore
                = new LmdbStore<>(root.resolve(revision).resolve(
                        ROUTE_DETAILS_STORE), RouteDetails.class
                );
        final Cache<RouteIdKey, RouteDetails> routeDetailsCache
                = new UnboundedCache<>(new InMemoryHashStore<>());
        final Store<RouteIdKey, RouteDetails> routeDetailsStore
                = new CachingStore(routeDetailsBackingStore,
                                   routeDetailsCache);
        final Reader routeReader = new FileReader(root
                .resolve(revision).resolve(GTFS_DIRECTORY)
                .resolve(ROUTES_FILE).toFile());

        final RouteDetailsDirectory routeDetailsDirectory
                = new GTFSReadingRouteDetailsDirectory(
                        routeDetailsStore, routeReader);
        return routeDetailsDirectory;
    }

    private static EntryPoints buildEntryPoints(
            final LocalDateTime earliestTime, final LocalDateTime latestTime,
            final ServiceTypeCalendar serviceTypeCalendar,
            final TripDetailsDirectory tripDetailsDirectory,
            final RouteDetailsDirectory routeDetailsDirectory,
            final StopTimesDirectory stopTimesDirectory,
            final ImmutableMap<String, TransitStop> stopIdMap)
            throws IOException, InterruptedException {
        final EntryPoints entryPoints = new DirectoryReadingEntryPoints(
                earliestTime, latestTime, stopTimesDirectory,
                routeDetailsDirectory, tripDetailsDirectory,
                serviceTypeCalendar, stopIdMap);
        return entryPoints;
    }
}
