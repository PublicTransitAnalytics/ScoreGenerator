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

import com.publictransitanalytics.scoregenerator.comparison.Comparison;
import com.publictransitanalytics.scoregenerator.comparison.ComparisonOperation;
import com.publictransitanalytics.scoregenerator.comparison.CalculationTransformer;
import com.publictransitanalytics.scoregenerator.workflow.Environment;
import com.publictransitanalytics.scoregenerator.workflow.Calculation;
import com.bitvantage.bitvantagecaching.LmdbStore;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import com.google.common.collect.BiMap;
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
import com.publictransitanalytics.scoregenerator.distanceclient.StoredDistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationDistanceKey;
import com.publictransitanalytics.scoregenerator.distanceclient.CachingDistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateRefiningReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.publictransitanalytics.scoregenerator.comparison.Extension;
import com.publictransitanalytics.scoregenerator.comparison.OperationDirection;
import com.publictransitanalytics.scoregenerator.comparison.Stop;
import com.publictransitanalytics.scoregenerator.comparison.Truncation;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopTimesDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.BoundsKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.TripIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.LocationKey;
import com.publictransitanalytics.scoregenerator.distanceclient.GraphhopperLocalDistanceClient;
import com.publictransitanalytics.scoregenerator.geography.EndpointDeterminer;
import com.publictransitanalytics.scoregenerator.geography.NearestPointEndpointDeterminer;
import com.publictransitanalytics.scoregenerator.geography.GeoJsonWaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetectorException;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.WaterStatus;
import com.publictransitanalytics.scoregenerator.distanceclient.BackwardPointOrderer;
import com.publictransitanalytics.scoregenerator.distanceclient.CompletePairGenerator;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.ForwardPointOrderer;
import com.publictransitanalytics.scoregenerator.distanceclient.PairGenerator;
import com.publictransitanalytics.scoregenerator.distanceclient.PermanentEstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.PointOrdererFactory;
import com.publictransitanalytics.scoregenerator.geography.StoredWaterDetector;
import com.publictransitanalytics.scoregenerator.output.ComparativeNetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.ComparativePointAccessibility;
import com.publictransitanalytics.scoregenerator.output.ComparativeTimeQualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.output.MapGenerator;
import com.publictransitanalytics.scoregenerator.output.NetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.PointAccessibility;
import com.publictransitanalytics.scoregenerator.output.TimeQualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.publishing.LocalFilePublisher;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import org.opensextant.geodesy.Geodetic2DBounds;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRangeExecutor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import com.publictransitanalytics.scoregenerator.workflow.Workflow;
import java.util.Map;
import java.util.Optional;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.schedule.patching.Deletion;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingTripCreator;
import com.publictransitanalytics.scoregenerator.schedule.patching.ExtensionType;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteExtension;
import com.publictransitanalytics.scoregenerator.scoring.CountScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.CountScoreCardFactory;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCardFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import com.publictransitanalytics.scoregenerator.workflow.TaskGroupIdentifier;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripProcessingTransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteTruncation;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingAlgorithm;
import com.publictransitanalytics.scoregenerator.workflow.ForwardMovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.MovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.ParallelTaskExecutor;
import com.publictransitanalytics.scoregenerator.workflow.RetrospectiveMovementAssembler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.NavigableMap;

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

    private static final int MAX_DEPTH = 10;

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
    private static final String WATER_STORE = "water_store";

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
        final Gson serializer = new GsonBuilder().setPrettyPrinting().create();

        final ArgumentParser parser = ArgumentParsers.newArgumentParser(
                "ScoreGenerator").defaultHelp(true)
                .description("Generate isochrone map data.");

        parser.addArgument("-l", "--tripLengths").action(Arguments.append());
        parser.addArgument("-i", "--samplingInterval");
        parser.addArgument("-d", "--baseDirectory");
        parser.addArgument("-k", "--backward").action(Arguments.storeTrue());
        parser.addArgument("-n", "--inMemCache").action(Arguments.storeTrue());
        parser.addArgument("-c", "--comparisonFile");
        parser.addArgument("-o", "--outputName");

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
        generateSampledNetworkAccessibilityParser.addArgument("-m",
                                                              "--samples");

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

        final String files = namespace.get("files");

        final String comparisonFile = namespace.get("comparisonFile");
        final Comparison comparison = (comparisonFile == null) ? null
                : serializer.fromJson(new String(Files.readAllBytes(
                        Paths.get(comparisonFile)), StandardCharsets.UTF_8),
                                      Comparison.class);

        final Boolean backwardObject = namespace.getBoolean("backward");
        final boolean backward
                = (backwardObject == null) ? false : backwardObject;

        final String samplingIntervalString = namespace.get(
                "samplingInterval");
        final Duration samplingInterval = (samplingIntervalString != null)
                ? Duration.ofMinutes(Long.valueOf(samplingIntervalString))
                : null;

        final String outputName = namespace.get("outputName");

        final String command = namespace.get("command");

        final LocalFilePublisher publisher = new LocalFilePublisher();

        final Store<BoundsKey, WaterStatus> waterStore = new LmdbStore<>(
                root.resolve(files).resolve(WATER_STORE), WaterStatus.class);

        final Path waterFilePath = root.resolve(WATER_BODIES_FILE);
        final WaterDetector waterDetector = new StoredWaterDetector(
                new GeoJsonWaterDetector(waterFilePath), waterStore);

        final NearestPointEndpointDeterminer endpointDeterminer
                = new NearestPointEndpointDeterminer();

        final Boolean inMemCacheObject = namespace.getBoolean("inMemCache");
        final StoreFactory storeFactory
                = (inMemCacheObject == null || inMemCacheObject == false)
                        ? new NoCacheStoreFactory()
                        : new UnboundedCacheStoreFactory();

        final Set<ModeType> modes = ImmutableSet.of(ModeType.TRANSIT,
                                                    ModeType.WALKING);

        final SectorTable sectorTable = generateSectors(waterDetector);

        final MapGenerator mapGenerator = new MapGenerator();

        try {
            if ("generatePointAccessibility".equals(command)) {
                final PathScoreCardFactory scoreCardFactory
                        = new PathScoreCardFactory();
                generatePointAccessibility(
                        namespace, root, files, storeFactory, scoreCardFactory,
                        sectorTable, backward, samplingInterval, durations,
                        waterDetector, endpointDeterminer, modes, comparison,
                        publisher, serializer, mapGenerator, outputName);
            } else if ("generateNetworkAccessibility".equals(command)) {
                final ScoreCardFactory scoreCardFactory
                        = new CountScoreCardFactory();

                generateNetworkAccessibility(
                        namespace, root, files, storeFactory, scoreCardFactory,
                        sectorTable, backward, samplingInterval, durations,
                        waterDetector, endpointDeterminer, modes, comparison,
                        publisher, serializer, mapGenerator, outputName);
            } else if ("generateSampledNetworkAccessibility".equals(command)) {
                final ScoreCardFactory scoreCardFactory
                        = new CountScoreCardFactory();

                generateSampledNetworkAccessibility(
                        namespace, root, files, storeFactory, scoreCardFactory,
                        sectorTable, backward, samplingInterval, durations,
                        waterDetector, endpointDeterminer, modes, comparison,
                        publisher, serializer, mapGenerator, outputName);
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

    private static TransitNetwork buildTransitNetwork(
            final StoreFactory storeFactory, final Path root,
            final String files, final LocalDateTime earliestTime,
            final LocalDateTime latestTime,
            final ImmutableBiMap<String, TransitStop> stopIdMap)
            throws IOException, InterruptedException {

        final ServiceTypeCalendar serviceTypeCalendar
                = buildServiceTypeCalendar(storeFactory, root, files);
        final TripDetailsDirectory tripDetailsDirectory
                = buildTripDetailsDirectory(storeFactory, root, files);
        final RouteDetailsDirectory routeDetailsDirectory
                = buildRouteDetailsDirectory(storeFactory, root, files);
        final StopTimesDirectory stopTimesDirectory
                = buildStopTimesDirectory(storeFactory, root, files);

        final TransitNetwork transitNetwork
                = new TripProcessingTransitNetwork(
                        new DirectoryReadingTripCreator(
                                earliestTime, latestTime, stopTimesDirectory,
                                routeDetailsDirectory, tripDetailsDirectory,
                                serviceTypeCalendar, stopIdMap));
        return transitNetwork;
    }

    private static void generateNetworkAccessibility(
            final Namespace namespace, final Path root, final String files,
            final StoreFactory storeFactory,
            final ScoreCardFactory scoreCardFactory,
            final SectorTable sectorTable, final boolean backward,
            final Duration samplingInterval,
            final NavigableSet<Duration> durations,
            final WaterDetector waterDetector,
            final NearestPointEndpointDeterminer endpointDeterminer,
            final Set<ModeType> allowedModes, final Comparison comparison,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName)
            throws IOException, InterruptedException, ExecutionException {

        final LocalDateTime startTime
                = LocalDateTime.parse(namespace.get("startTime"));

        final Duration span = Duration.parse(namespace.get("span"));

        final LocalDateTime endTime = startTime.plus(span);

        final Set<Sector> measuredSectors = sectorTable.getSectors();
        final Set<PointLocation> centerPoints = measuredSectors.stream()
                .map(sector -> new Landmark(
                sector, sector.getCanonicalPoint()))
                .collect(Collectors.toSet());
        final BiMap<Optional<Comparison>, Calculation<CountScoreCard>> result
                = calculate(root, files, storeFactory, scoreCardFactory,
                            sectorTable, centerPoints, startTime, endTime,
                            samplingInterval, backward, endpointDeterminer,
                            waterDetector, durations.last(), allowedModes,
                            comparison);
        publishNetworkAccessibility(comparison, result, sectorTable,
                                    centerPoints, startTime, endTime,
                                    durations, samplingInterval, backward,
                                    publisher, serializer, mapGenerator,
                                    outputName);
    }

    private static <S extends ScoreCard> BiMap<Optional<Comparison>, Calculation<S>> calculate(
            final Path root, final String files,
            final StoreFactory storeFactory,
            final ScoreCardFactory<S> scoreCardFactory,
            final SectorTable sectorTable,
            final Set<PointLocation> centerPoints,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration samplingInterval, final boolean backward,
            final EndpointDeterminer endpointDeterminer,
            final WaterDetector waterDetector,
            final Duration longestDuration, final Set<ModeType> allowedModes,
            final Comparison comparison)
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

        final TransitNetwork transitNetwork = buildTransitNetwork(
                storeFactory, root, files, earliestTime, latestTime, stopIdMap);

        final RiderFactory riderFactory;
        final TimeTracker timeTracker;
        final PointOrdererFactory ordererFactory;
        final DistanceClient distanceClient;
        final MovementAssembler assembler;
        if (!backward) {
            riderFactory = new ForwardRiderFactory(transitNetwork);
            timeTracker = new ForwardTimeTracker();
            ordererFactory = (point, consideredPoint)
                    -> new ForwardPointOrderer(point, consideredPoint);
            distanceClient = buildDistanceClient(
                    root, storeFactory, endpointDeterminer, ordererFactory,
                    waterDetector);

            assembler = new ForwardMovementAssembler();
        } else {
            riderFactory = new RetrospectiveRiderFactory(
                    transitNetwork);
            timeTracker = new BackwardTimeTracker();
            ordererFactory = (point, consideredPoint)
                    -> new BackwardPointOrderer(point, consideredPoint);
            distanceClient = buildDistanceClient(
                    root, storeFactory, endpointDeterminer, ordererFactory,
                    waterDetector);
            assembler = new RetrospectiveMovementAssembler();
        }

        final DistanceEstimator distanceEstimator
                = buildDistanceEstimator(
                        sectorTable, endpointDeterminer, centerPoints,
                        stopIdMap, longestDuration, storeFactory, root, files,
                        locationIdMap);

        final Set<TaskGroupIdentifier> taskGroups = getTaskGroups(centerPoints);
        final NavigableSet<LocalDateTime> taskTimes = getTaskTimes(
                startTime, endTime, samplingInterval);

        final ReachabilityClient reachabilityClient
                = buildReachabilityClient(distanceClient, distanceEstimator,
                                          timeTracker);
        final S scoreCard = scoreCardFactory.makeScoreCard(
                taskTimes.size() * taskGroups.size());

        final Calculation calculation = new Calculation(
                taskGroups, taskTimes, scoreCard, timeTracker,
                assembler, allowedModes, transitNetwork, backward,
                longestDuration, ESTIMATE_WALK_METERS_PER_SECOND,
                endpointDeterminer, distanceClient, sectorTable.getSectors(),
                stopIdMap.values(), centerPoints, distanceEstimator,
                reachabilityClient, riderFactory);
        final ImmutableBiMap.Builder<Optional<Comparison>, Calculation<S>> resultBuilder
                = ImmutableBiMap.builder();
        resultBuilder.put(Optional.empty(), calculation);

        final Environment environment = new Environment(
                sectorTable, longestDuration, MAX_DEPTH);

        if (comparison != null) {

            final Calculation trialCalculation = makeTrialCalculation(
                    calculation, scoreCardFactory, comparison, sectorTable,
                    stopIdMap);
            log.info(
                    "Trial in service time = {}; original in service time = {} ",
                    trialCalculation.getTransitNetwork()
                            .getInServiceTime(),
                    calculation.getTransitNetwork().getInServiceTime());
            resultBuilder.put(Optional.of(comparison), trialCalculation);
        }

        final BiMap<Optional<Comparison>, Calculation<S>> calculations
                = resultBuilder.build();
        try {

            final Workflow workflow = new ParallelTaskExecutor(
                    new DynamicProgrammingRangeExecutor(
                            new DynamicProgrammingAlgorithm(),
                            environment));

            workflow.calculate(calculations.values());
            return calculations;

        } finally {
            distanceEstimator.close();
        }
    }

    private static Calculation makeTrialCalculation(
            final Calculation calculation,
            final ScoreCardFactory scoreCardFactory,
            final Comparison comparison,
            final SectorTable sectorTable,
            final BiMap<String, TransitStop> stopIdMap)
            throws InterruptedException {
        final CalculationTransformer transformer
                = new CalculationTransformer(calculation, scoreCardFactory);
        final Map<String, TransitStop> allStopsById = new HashMap<>();
        allStopsById.putAll(stopIdMap);
        final List<ComparisonOperation> operations
                = comparison.getOperations();

        if (operations != null) {
            for (final ComparisonOperation operation : operations) {
                final String route = operation.getRoute();

                switch (operation.getOperator()) {
                case DELETE:
                    final Deletion deletion
                            = new Deletion(route);
                    transformer.addTripPatch(deletion);
                    break;
                case ADD:
                    final Stop stopData = operation.getStop();
                    final Geodetic2DPoint location
                            = new Geodetic2DPoint(stopData.getLocation());
                    final Sector sector = sectorTable.findSector(location);
                    final String name = stopData.getStopId();
                    final TransitStop newStop = new TransitStop(
                            sector, name, name, location);
                    allStopsById.put(name, newStop);
                    transformer.addStop(newStop);
                    break;
                case EXTEND:
                    final Extension extension = operation.getExtension();

                    final RouteExtension routeExtension = getRouteExtension(
                            route, extension, allStopsById);
                    transformer.addTripPatch(routeExtension);
                    break;
                case TRUNCATE:
                    final Truncation truncation = operation.getTruncation();

                    final RouteTruncation routeTruncation = getRouteTruncation(
                            route, truncation, allStopsById);
                    transformer.addTripPatch(routeTruncation);
                    break;
                }
            }
        }
        return transformer.transform();
    }

    private static RouteTruncation getRouteTruncation(
            final String route, final Truncation truncation,
            final Map<String, TransitStop> stopIdMap) {
        final ExtensionType type = mapDirection(truncation.getType());
        final TransitStop referenceStop = stopIdMap.get(
                truncation.getReferenceStopId());
        final RouteTruncation routeTruncation = new RouteTruncation(
                route, referenceStop, type);
        return routeTruncation;
    }

    private static RouteExtension getRouteExtension(
            final String route, final Extension extension,
            final Map<String, TransitStop> stopIdMap) {
        final ExtensionType type
                = mapDirection(extension.getType());
        final TransitStop referenceStop = stopIdMap.get(
                extension.getReferenceStopId());

        final NavigableMap<Duration, TransitStop> sequence
                = mapSequence(extension.getSequence(), stopIdMap);
        final RouteExtension routeExtension = new RouteExtension(
                route, referenceStop, type, sequence);
        return routeExtension;
    }

    private static void generateSampledNetworkAccessibility(
            final Namespace namespace, final Path root, final String files,
            final StoreFactory storeFactory,
            final ScoreCardFactory scoreCardFactory,
            final SectorTable sectorTable, final boolean backward,
            final Duration samplingInterval,
            final NavigableSet<Duration> durations,
            final WaterDetector waterDetector,
            final EndpointDeterminer endpointDeterminer,
            final Set<ModeType> allowedModes, final Comparison comparison,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName)
            throws IOException, InterruptedException, ExecutionException {

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

        final BiMap<Optional<Comparison>, Calculation<CountScoreCard>> result
                = calculate(root, files, storeFactory, scoreCardFactory,
                            sectorTable, centerPoints, startTime, endTime,
                            samplingInterval, backward, endpointDeterminer,
                            waterDetector, durations.last(), allowedModes,
                            comparison);
        publishNetworkAccessibility(comparison, result, sectorTable,
                                    centerPoints, startTime, endTime,
                                    durations, samplingInterval, backward,
                                    publisher, serializer, mapGenerator,
                                    outputName);

    }

    private static void publishNetworkAccessibility(
            final Comparison comparison,
            final BiMap<Optional<Comparison>, Calculation<CountScoreCard>> calculations,
            final SectorTable sectorTable,
            final Set<PointLocation> centerPoints,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final NavigableSet<Duration> durations,
            final Duration samplingInterval, final boolean backward,
            final LocalFilePublisher publisher, Gson serializer,
            final MapGenerator mapGenerator, final String outputName)
            throws InterruptedException, IOException {
        final Calculation calculation
                = calculations.get(Optional.<Comparison>empty());

        final ScoreCard scoreCard = calculation.getScoreCard();
        final int taskCount = calculation.getTaskCount();
        if (comparison == null) {
            final NetworkAccessibility map = new NetworkAccessibility(
                    taskCount, scoreCard, sectorTable, centerPoints,
                    startTime, endTime, durations.last(), samplingInterval,
                    backward);
            publisher.publish(outputName, serializer.toJson(map));
            mapGenerator.makeRangeMap(sectorTable, scoreCard, 0, 0.2,
                                      outputName);
        } else {
            for (final Optional<Comparison> trialComparison
                         : calculations.keySet()) {
                if (trialComparison.isPresent()) {
                    final String name = comparison.getName();
                    final Calculation trialCalculation
                            = calculations.get(trialComparison);
                    final ScoreCard trialScoreCard
                            = trialCalculation.getScoreCard();
                    final int trialTaskCount = trialCalculation.getTaskCount();
                    final ComparativeNetworkAccessibility map
                            = new ComparativeNetworkAccessibility(
                                    taskCount, trialTaskCount, scoreCard,
                                    trialScoreCard, sectorTable,
                                    centerPoints, startTime, endTime,
                                    durations.last(), samplingInterval,
                                    backward, name);
                    publisher.publish(outputName, serializer.toJson(map));
                    mapGenerator.makeComparativeMap(
                            outputName, sectorTable, scoreCard, trialScoreCard,
                            0.2, outputName);
                }
            }
        }
    }

    private static void generatePointAccessibility(
            final Namespace namespace, final Path root, final String files,
            final StoreFactory storeFactory,
            final PathScoreCardFactory scoreCardFactory,
            final SectorTable sectorTable, final boolean backward,
            final Duration samplingInterval,
            final NavigableSet<Duration> durations,
            final WaterDetector waterDetector,
            final NearestPointEndpointDeterminer endpointDeterminer,
            final Set<ModeType> allowedModes, final Comparison comparison,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName)
            throws IOException, InterruptedException, ExecutionException {

        final String coordinateString = namespace.get("coordinate");
        final Geodetic2DPoint centerCoordinate = (coordinateString == null)
                ? null : new Geodetic2DPoint(coordinateString);
        final Landmark centerPoint = buildCenterPoint(
                sectorTable, centerCoordinate);

        final LocalDateTime startTime
                = LocalDateTime.parse(namespace.get("startTime"));

        final String spanString = namespace.get("span");

        final Duration span = (spanString == null) ? null : Duration.parse(
                spanString);

        final LocalDateTime endTime
                = (span == null) ? null : startTime.plus(span);

        final Map<Optional<Comparison>, Calculation<PathScoreCard>> calculations
                = calculate(root, files, storeFactory, scoreCardFactory,
                            sectorTable, Collections.singleton(centerPoint),
                            startTime, endTime, samplingInterval, backward,
                            endpointDeterminer, waterDetector, durations.last(),
                            allowedModes, comparison);
        final Calculation<PathScoreCard> baseCalculation
                = calculations.get(Optional.<Comparison>empty());
        final PathScoreCard scoreCard = baseCalculation.getScoreCard();
        final int taskCount = baseCalculation.getTaskCount();
        if (comparison == null) {
            if (endTime != null) {
                final PointAccessibility map = new PointAccessibility(
                        taskCount, scoreCard, sectorTable, centerPoint,
                        startTime, endTime, samplingInterval, durations.last(),
                        backward);
                publisher.publish(outputName, serializer.toJson(map));
            } else {
                final TimeQualifiedPointAccessibility map
                        = new TimeQualifiedPointAccessibility(
                                scoreCard, sectorTable, centerPoint,
                                startTime, durations.last(), backward);
                publisher.publish(outputName, serializer.toJson(map));
            }
            mapGenerator.makeRangeMap(sectorTable, scoreCard, 0, 1, outputName);
        } else {
            for (final Optional<Comparison> trialComparison
                         : calculations.keySet()) {
                if (trialComparison.isPresent()) {
                    final String name = comparison.getName();
                    final Calculation<PathScoreCard> trialCalculation
                            = calculations.get(trialComparison);
                    final PathScoreCard trialScoreCard
                            = trialCalculation.getScoreCard();
                    final int trialTaskCount = trialCalculation.getTaskCount();
                    if (endTime != null) {
                        final ComparativePointAccessibility map
                                = new ComparativePointAccessibility(
                                        taskCount, trialTaskCount, scoreCard,
                                        trialScoreCard, sectorTable,
                                        centerPoint, startTime, endTime,
                                        samplingInterval, durations.last(),
                                        backward, name);
                        publisher.publish(outputName, serializer.toJson(map));
                    } else {
                        final ComparativeTimeQualifiedPointAccessibility map
                                = new ComparativeTimeQualifiedPointAccessibility(
                                        scoreCard, trialScoreCard, sectorTable,
                                        centerPoint, startTime,
                                        durations.last(), backward, name);
                        publisher.publish(outputName, serializer.toJson(map));
                    }
                    mapGenerator.makeComparativeMap(
                            outputName, sectorTable, scoreCard, trialScoreCard,
                            0.2, outputName);
                }
            }
        }

    }

    private static SectorTable generateSectors(
            final WaterDetector waterDetector) throws InterruptedException {
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
                log.debug(
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
        final Store<StopIdKey, StopDetails> stopDetailsStore
                = new LmdbStore<>(baseDirectory.resolve(revision).resolve(
                        STOP_DETAILS_STORE), StopDetails.class);

        final Reader stopDetailsReader = new FileReader(
                baseDirectory.resolve(revision).resolve(GTFS_DIRECTORY).resolve(
                        STOPS_FILE).toFile());

        final GTFSReadingStopDetailsDirectory stopDetailsDirectory
                = new GTFSReadingStopDetailsDirectory(stopDetailsStore,
                                                      stopDetailsReader);
        return stopDetailsDirectory;
    }

    private static DistanceClient buildDistanceClient(
            final Path baseDirectory, final StoreFactory storeFactory,
            final EndpointDeterminer endpointDeterminer,
            final PointOrdererFactory ordererFactory,
            final WaterDetector waterDetector) {
        final Store<DistanceCacheKey, WalkingDistanceMeasurement> walkingDistanceStore
                = storeFactory.getStore(
                        baseDirectory.resolve(WALKING_DISTANCE_STORE),
                        WalkingDistanceMeasurement.class);

        final GraphhopperLocalDistanceClient graphhopperDistanceClient
                = new GraphhopperLocalDistanceClient(
                        ordererFactory,
                        baseDirectory.resolve(SEATTLE_OSM_FILE),
                        baseDirectory.resolve(GRAPHHOPPER_DIRECTORY),
                        endpointDeterminer);
        final DistanceClient distanceClient = new CachingDistanceClient(
                ordererFactory, walkingDistanceStore,
                graphhopperDistanceClient);

        return distanceClient;
    }

    private static ReachabilityClient buildReachabilityClient(
            final DistanceClient distanceClient,
            final DistanceEstimator distanceEstimator,
            final TimeTracker timeTracker) {
        final ReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        distanceClient, distanceEstimator, timeTracker,
                        ESTIMATE_WALK_METERS_PER_SECOND);
        return reachabilityClient;
    }

    private static DistanceEstimator buildDistanceEstimator(
            final SectorTable sectorTable,
            final EndpointDeterminer endpointDeterminer,
            final Set<PointLocation> centers,
            final ImmutableBiMap<String, TransitStop> stopIdMap,
            final Duration maxDuration, final StoreFactory storeFactory,
            final Path baseDirectory, final String revision,
            final ImmutableBiMap<String, VisitableLocation> locationIdMap)
            throws InterruptedException {
        final Path candidateDistancesStorePath = baseDirectory.resolve(revision)
                .resolve(CANDIDATE_STOP_DISTANCES_STORE);
        final RangedStore<LocationDistanceKey, String> candidateStopDistancesStore
                = storeFactory.getRangedStore(
                        candidateDistancesStorePath,
                        new LocationDistanceKey.Materializer(), String.class);

        final Path maxCandidateDistanceStorePath = baseDirectory.resolve(
                revision).resolve(MAX_CANDIDATE_STOP_DISTANCE_STORE);
        final Store<LocationKey, Double> maxCandidateDistanceStore
                = storeFactory.getStore(maxCandidateDistanceStorePath,
                                        Double.class);

        final double maxWalkingDistance
                = ESTIMATE_WALK_METERS_PER_SECOND * maxDuration.getSeconds();
        final EstimateStorage estimateStorage = new PermanentEstimateStorage(
                maxCandidateDistanceStore, candidateStopDistancesStore,
                locationIdMap);
        final PairGenerator pairGenerator = new CompletePairGenerator(
                sectorTable.getSectors(), stopIdMap.values(), centers);

        final DistanceEstimator distanceEstimator = new StoredDistanceEstimator(
                pairGenerator, maxWalkingDistance, endpointDeterminer,
                estimateStorage);
        return distanceEstimator;
    }

    private static ServiceTypeCalendar buildServiceTypeCalendar(
            final StoreFactory storeFactory, final Path root,
            final String revision) throws IOException, InterruptedException {
        final Store<DateKey, ServiceSet> serviceTypesStore
                = storeFactory.getStore(root.resolve(revision).resolve(
                        SERVICE_TYPES_STORE), ServiceSet.class);

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
        final Store<TripGroupKey, TripDetails> tripDetailsStore
                = storeFactory.getStore(root.resolve(revision).resolve(
                        TRIP_DETAILS_STORE), TripDetails.class);

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
        final Path tripSequenceStorePath
                = root.resolve(revision).resolve(TRIP_SEQUENCE_STORE);
        final RangedStore<TripSequenceKey, TripStop> tripSequenceStore
                = storeFactory.getRangedStore(
                        tripSequenceStorePath,
                        new TripSequenceKey.Materializer(), TripStop.class);

        final Path stopTimesStorePath = root.resolve(revision)
                .resolve(STOP_TIMES_STORE);
        final RangedStore<StopTimeKey, TripStop> stopTimesStore
                = storeFactory.getRangedStore(stopTimesStorePath,
                                              new StopTimeKey.Materializer(),
                                              TripStop.class);

        final Store<TripIdKey, TripId> tripsStore = storeFactory.getStore(
                root.resolve(revision).resolve(TRIPS_STORE), TripId.class);

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
            final StoreFactory storeFactory, final Path root,
            final String revision) throws IOException, InterruptedException {
        final Store<RouteIdKey, RouteDetails> routeDetailsStore
                = storeFactory.getStore(root.resolve(revision).resolve(
                        ROUTE_DETAILS_STORE), RouteDetails.class);

        final Reader routeReader = new FileReader(root
                .resolve(revision).resolve(GTFS_DIRECTORY)
                .resolve(ROUTES_FILE).toFile());

        final RouteDetailsDirectory routeDetailsDirectory
                = new GTFSReadingRouteDetailsDirectory(
                        routeDetailsStore, routeReader);
        return routeDetailsDirectory;
    }

    private static Set<TaskGroupIdentifier> getTaskGroups(
            final Set<PointLocation> centerPoints) {
        return centerPoints.stream()
                .map(point -> new TaskGroupIdentifier(point))
                .collect(Collectors.toSet());
    }

    private static NavigableSet<LocalDateTime> getTaskTimes(
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration samplingInterval) {
        final ImmutableSortedSet.Builder<LocalDateTime> builder
                = ImmutableSortedSet.naturalOrder();
        if (endTime == null) {
            builder.add(startTime);
        } else {
            LocalDateTime time = startTime;
            while (time.isBefore(endTime)) {
                builder.add(time);
                time = time.plus(samplingInterval);
            }
        }
        return builder.build();
    }

    private static ExtensionType mapDirection(
            final com.publictransitanalytics.scoregenerator.comparison.OperationDirection type) {
        switch (type) {
        case BEFORE_FIRST:
            return ExtensionType.BEFORE_FIRST;
        case AFTER_LAST:
            return ExtensionType.AFTER_LAST;
        default:
            throw new ScoreGeneratorFatalException(String.format(
                    "Cannot convert extension type %s", type));

        }
    }

    private static NavigableMap<Duration, TransitStop> mapSequence(
            final Map<Integer, String> sequence,
            final Map<String, TransitStop> stopIdMap) {
        final ImmutableSortedMap.Builder<Duration, TransitStop> builder
                = ImmutableSortedMap.naturalOrder();
        for (final int secondOffset : sequence.keySet()) {
            final Duration offset = Duration.ofSeconds(secondOffset);
            final TransitStop stop = stopIdMap.get(sequence.get(secondOffset));
            builder.put(offset, stop);
        }
        return builder.build();
    }
}
