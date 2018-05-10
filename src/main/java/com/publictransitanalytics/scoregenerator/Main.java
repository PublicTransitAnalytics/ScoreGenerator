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

import com.bitvantage.bitvantagecaching.GsonSerializer;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Serializer;
import com.bitvantage.bitvantagecaching.Store;
import com.publictransitanalytics.scoregenerator.console.NetworkConsoleFactory;
import com.publictransitanalytics.scoregenerator.console.DummyNetworkConsole;
import com.publictransitanalytics.scoregenerator.console.InteractiveNetworkConsole;
import com.publictransitanalytics.scoregenerator.console.NetworkConsole;
import com.publictransitanalytics.scoregenerator.comparison.OperationDescription;
import com.publictransitanalytics.scoregenerator.workflow.Environment;
import com.publictransitanalytics.scoregenerator.workflow.Calculation;
import com.google.common.collect.BiMap;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.publictransitanalytics.scoregenerator.comparison.ComparisonOperation;
import com.publictransitanalytics.scoregenerator.comparison.Extension;
import com.publictransitanalytics.scoregenerator.comparison.Reroute;
import com.publictransitanalytics.scoregenerator.comparison.SequenceItem;
import com.publictransitanalytics.scoregenerator.comparison.Stop;
import com.publictransitanalytics.scoregenerator.comparison.Truncation;
import com.publictransitanalytics.scoregenerator.publishing.LocalFileManager;
import com.publictransitanalytics.scoregenerator.publishing.DownloaderException;
import com.publictransitanalytics.scoregenerator.publishing.RemoteFileManager;
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
import com.publictransitanalytics.scoregenerator.datalayer.directories.LocalServiceDataDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceDataDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.StopDetails;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridInfo;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridPointAssociation;
import com.publictransitanalytics.scoregenerator.datalayer.environment.SectorInfo;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridPointAssociationKey;
import com.publictransitanalytics.scoregenerator.datalayer.environment.SectorKey;
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.environment.StoredGrid;
import com.publictransitanalytics.scoregenerator.environment.ReadingSegmentFinder;
import com.publictransitanalytics.scoregenerator.geography.GeoJsonInEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetectorException;
import com.publictransitanalytics.scoregenerator.location.Center;
import com.publictransitanalytics.scoregenerator.output.ComparativeNetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.ComparativePointAccessibility;
import com.publictransitanalytics.scoregenerator.output.ComparativeTimeQualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.output.MapGenerator;
import com.publictransitanalytics.scoregenerator.output.NetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.PointAccessibility;
import com.publictransitanalytics.scoregenerator.output.TimeQualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.workflow.ProgressiveRangeExecutor;
import java.util.ArrayList;
import java.util.Collections;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import com.publictransitanalytics.scoregenerator.workflow.Workflow;
import java.util.Map;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCardFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import com.publictransitanalytics.scoregenerator.walking.BackwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingAlgorithm;
import com.publictransitanalytics.scoregenerator.workflow.ForwardMovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.MovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.ParallelTaskExecutor;
import com.publictransitanalytics.scoregenerator.workflow.RetrospectiveMovementAssembler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.patching.Patch;
import com.publictransitanalytics.scoregenerator.schedule.patching.ReferenceDirection;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteDeletion;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteExtension;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteReroute;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteSequenceItem;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteTruncation;
import com.publictransitanalytics.scoregenerator.schedule.patching.StopDeletion;
import com.publictransitanalytics.scoregenerator.scoring.CountScoreCardFactory;
import java.util.function.Function;
import com.publictransitanalytics.scoregenerator.publishing.FileManager;
import com.publictransitanalytics.scoregenerator.publishing.S3Client;
import com.publictransitanalytics.scoregenerator.publishing.TarGzCompressor;

@Slf4j
/**
 * Parses inputs, builds up infrastructure, and delegates to solve reachability
 * problems.
 *
 * @author Public Transit Analytics
 */
public class Main {

    private static final double ESTIMATE_WALK_METERS_PER_SECOND = 2.0;
    private static final int RESOLUTION_METERS = 80;

    private static final String OSM_FILE = "environment.osm.pbf";

    private static final String GRID_INFO_STORE = "grid_info_store";
    private static final String SECTOR_INFO_STORE = "sector_info_store";
    private static final String GRID_POINT_ASSOCATION_STORE
            = "grid_point_association_store";

    private static final String WATER_BODIES_FILE = "water.json";
    private static final String BORDER_FILE = "border.json";

    public static void main(String[] args) throws FileNotFoundException,
            IOException, ArgumentParserException, InterruptedException,
            ExecutionException, InEnvironmentDetectorException {
        final Gson serializer = new GsonBuilder().setPrettyPrinting().create();

        final ArgumentParser parser = ArgumentParsers.newArgumentParser(
                "ScoreGenerator").defaultHelp(true)
                .description("Generate isochrone map data.");

        parser.addArgument("-l", "--tripLengths").action(Arguments.append());
        parser.addArgument("-i", "--samplingInterval");
        parser.addArgument("-s", "--span");
        parser.addArgument("-r", "--root");
        parser.addArgument("-e", "--bucket");
        parser.addArgument("-f", "--fileSet");
        parser.addArgument("-k", "--backward").action(Arguments.storeTrue());
        parser.addArgument("-n", "--inMemCache").action(Arguments.storeTrue());
        parser.addArgument("-t", "--interactive").action(Arguments.storeTrue());
        parser.addArgument("-b", "--baseParameters");
        parser.addArgument("-u", "--bounds");
        parser.addArgument("-c", "--comparisonParameters");
        parser.addArgument("-o", "--outputName");

        final Subparsers subparsers = parser.addSubparsers().dest("command");

        subparsers.addParser("generateNetworkAccessibility");
        final Subparser generateSampledNetworkAccessibilityParser
                = subparsers.addParser("generateSampledNetworkAccessibility");
        generateSampledNetworkAccessibilityParser.addArgument("-m",
                                                              "--samples");

        final Subparser generatePointAccessibilityParser = subparsers.addParser(
                "generatePointAccessibility");
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

        final String bucket = namespace.get("bucket");
        final String rootDirectoryString = namespace.get("root");
        final String fileSet = namespace.get("fileSet");

        final FileManager fileManager;
        if (rootDirectoryString != null) {
            fileManager = new LocalFileManager(rootDirectoryString,
                                               fileSet);
        } else if (bucket != null) {
            final TarGzCompressor compressor = new TarGzCompressor();
            try {
                fileManager = new RemoteFileManager(
                        new S3Client(compressor, bucket), fileSet);
            } catch (DownloaderException e) {
                throw new ScoreGeneratorFatalException(e);
            }
        } else {
            throw new ScoreGeneratorFatalException(
                    "Either --root or --bucket must be specified");
        }
        final Path root = fileManager.getRoot();

        final String baseFile = namespace.get("baseParameters");
        final OperationDescription baseDescription = serializer.fromJson(
                new String(Files.readAllBytes(Paths.get(baseFile)),
                           StandardCharsets.UTF_8), OperationDescription.class);

        final String comparisonFile = namespace.get("comparisonParameters");
        final OperationDescription comparisonDescription
                = (comparisonFile == null) ? null : serializer.fromJson(
                                new String(Files.readAllBytes(
                                        Paths.get(comparisonFile)),
                                           StandardCharsets.UTF_8),
                                OperationDescription.class);

        final Boolean backwardObject = namespace.getBoolean("backward");
        final boolean backward
                = (backwardObject == null) ? false : backwardObject;

        final Boolean interactiveObject = namespace.getBoolean("interactive");
        final boolean interactive
                = (interactiveObject == null) ? false : interactiveObject;

        final NetworkConsoleFactory consoleFactory;
        if (interactive) {
            consoleFactory = (network, stopIdMap)
                    -> new InteractiveNetworkConsole(network, stopIdMap);
        } else {
            consoleFactory = (network, stopIdMap) -> new DummyNetworkConsole();
        }

        final String samplingIntervalString = namespace.get(
                "samplingInterval");
        final Duration samplingInterval = (samplingIntervalString != null)
                ? Duration.ofMinutes(Long.valueOf(samplingIntervalString))
                : null;

        final String spanString = namespace.get("span");
        final Duration span = (spanString != null)
                ? Duration.parse(spanString) : null;

        final String outputName = namespace.get("outputName");

        final String command = namespace.get("command");

        final ImmutableSet.Builder<String> fileNamesBuilder
                = ImmutableSet.builder();
        fileNamesBuilder.add(baseDescription.getFiles());
        if (comparisonDescription != null) {
            fileNamesBuilder.add(comparisonDescription.getFiles());
        }
        final Set<String> fileNames = fileNamesBuilder.build();

        final Boolean inMemCacheObject = namespace.getBoolean("inMemCache");
        final StoreFactory storeFactory
                = (inMemCacheObject == null || inMemCacheObject == false)
                        ? new NoCacheStoreFactory()
                        : new UnboundedCacheStoreFactory();

        final Map<String, ServiceDataDirectory> serviceDirectoriesMap
                = new HashMap<>();
        for (final String fileName : fileNames) {
            if (!serviceDirectoriesMap.containsKey(fileName)) {
                final ServiceDataDirectory directory
                        = new LocalServiceDataDirectory(root, fileName,
                                                        storeFactory);
                serviceDirectoriesMap.put(fileName, directory);
            }
        }

        final String boundsString = namespace.get("bounds");
        final GeoBounds bounds = parseBounds(boundsString);
        final Grid grid = getGrid(root, bounds, storeFactory);

        final MapGenerator mapGenerator = new MapGenerator(fileManager);

        final TimeTracker timeTracker;
        if (!backward) {
            timeTracker = new ForwardTimeTracker();
        } else {
            timeTracker = new BackwardTimeTracker();
        }

        if ("generatePointAccessibility".equals(command)) {

            generatePointAccessibility(
                    namespace, baseDescription, backward, samplingInterval,
                    span, durations, grid, serviceDirectoriesMap,
                    comparisonDescription, fileManager, serializer,
                    mapGenerator, outputName, consoleFactory);
        } else if ("generateNetworkAccessibility".equals(command)) {
            final ScoreCardFactory scoreCardFactory
                    = new CountScoreCardFactory();
            final Set<Center> centers = getAllCenters(grid);

            final BiMap<OperationDescription, Calculation<ScoreCard>> result
                    = Main.<ScoreCard>runComparison(
                            baseDescription, scoreCardFactory, centers,
                            samplingInterval, span, backward, timeTracker,
                            grid, serviceDirectoriesMap, durations.last(),
                            comparisonDescription, consoleFactory);

            final Set<Sector> sectors = grid.getReachableSectors();
            publishNetworkAccessibility(baseDescription, comparisonDescription,
                                        result, grid, sectors, false,
                                        durations, span, samplingInterval,
                                        backward, fileManager, serializer,
                                        mapGenerator, outputName);
        } else if ("generateSampledNetworkAccessibility".equals(command)) {

            final ScoreCardFactory scoreCardFactory
                    = new CountScoreCardFactory();

            final int samples = Integer.valueOf(namespace.get("samples"));

            final Set<Sector> allReachableSectors = grid.getReachableSectors();
            final List<Sector> sectorList
                    = new ArrayList<>(allReachableSectors);
            Collections.shuffle(sectorList);
            final Set<Sector> sampleSectors
                    = ImmutableSet.copyOf(sectorList.subList(0, samples));
            final Set<Center> centers = getSampleCenters(sampleSectors, grid);

            final BiMap<OperationDescription, Calculation<ScoreCard>> result
                    = Main.<ScoreCard>runComparison(
                            baseDescription, scoreCardFactory, centers,
                            samplingInterval, span, backward, timeTracker, grid,
                            serviceDirectoriesMap, durations.last(),
                            comparisonDescription, consoleFactory);
            publishNetworkAccessibility(baseDescription, comparisonDescription,
                                        result, grid, sampleSectors, true,
                                        durations, span, samplingInterval,
                                        backward, fileManager, serializer,
                                        mapGenerator, outputName);
        }

        fileManager.uploadFileSet(fileSet);
    }

    private static Set<Center> getSampleCenters(
            final Set<Sector> samples, final Grid grid) {
        final ImmutableSet.Builder<Center> builder
                = ImmutableSet.builder();
        for (final Sector sector : samples) {
            builder.add(new Center(sector, grid.getGridPoints(sector)));
        }
        return builder.build();
    }

    private static Set<Center> getAllCenters(final Grid grid) {
        final ImmutableSet.Builder<Center> builder = ImmutableSet
                .builder();
        for (final Sector sector : grid.getReachableSectors()) {
            builder.add(new Center(sector, grid.getGridPoints(sector)));
        }
        return builder.build();
    }

    private static List<Patch> getTripPatches(
            final OperationDescription description,
            final BiMap<String, TransitStop> stopIdMap) {
        final ImmutableList.Builder<Patch> builder = ImmutableList.builder();
        final List<ComparisonOperation> operations
                = description.getOperations();
        if (operations != null) {
            for (final ComparisonOperation operation : operations) {
                final String route = operation.getRoute();
                final Stop stop = operation.getStop();

                switch (operation.getOperator()) {
                case DELETE:
                    if (route == null && stop != null) {
                        final TransitStop deletedStop
                                = stopIdMap.get(stop.getStopId());
                        final StopDeletion deletion
                                = new StopDeletion(deletedStop);
                        builder.add(deletion);
                    } else if (route != null && stop == null) {
                        final RouteDeletion deletion = new RouteDeletion(route);
                        builder.add(deletion);
                    }
                    break;
                case ADD:
                    break;
                case EXTEND:
                    final Extension extension = operation.getExtension();

                    final RouteExtension routeExtension = getRouteExtension(
                            route, extension, stopIdMap);
                    builder.add(routeExtension);
                    break;
                case TRUNCATE:
                    final Truncation truncation = operation.getTruncation();

                    final RouteTruncation routeTruncation = getRouteTruncation(
                            route, truncation, stopIdMap);
                    builder.add(routeTruncation);
                    break;
                case REROUTE:
                    final Reroute reroute = operation.getReroute();
                    final RouteReroute routeReroute = getRouteReroute(
                            route, reroute, stopIdMap);
                    builder.add(routeReroute);
                    break;
                }
            }
        }
        return builder.build();
    }

    private static RouteTruncation getRouteTruncation(
            final String route, final Truncation truncation,
            final Map<String, TransitStop> stopIdMap) {
        final ReferenceDirection type = mapDirection(truncation.getDirection());
        final TransitStop referenceStop = stopIdMap.get(
                truncation.getReferenceStopId());
        final RouteTruncation routeTruncation = new RouteTruncation(
                route, referenceStop, type);
        return routeTruncation;
    }

    private static RouteExtension getRouteExtension(
            final String route, final Extension extension,
            final Map<String, TransitStop> stopIdMap) {
        final ReferenceDirection type = mapDirection(extension.getDirection());
        final TransitStop referenceStop = stopIdMap.get(
                extension.getReferenceStopId());

        final List<RouteSequenceItem> sequence
                = mapSequence(extension.getSequence(), stopIdMap);
        final RouteExtension routeExtension = new RouteExtension(
                route, referenceStop, type, sequence);
        return routeExtension;
    }

    private static RouteReroute getRouteReroute(
            final String route, final Reroute reroute,
            final Map<String, TransitStop> stopIdMap) {
        final ReferenceDirection type = mapDirection(reroute.getDirection());
        final TransitStop referenceStop
                = stopIdMap.get(reroute.getReferenceStopId());
        final String returnStopId = reroute.getReturnStopId();
        final TransitStop returnStop = (returnStopId == null) ? null : stopIdMap
                .get(returnStopId);
        final String deltaString = reroute.getReturnDelta();
        final Duration returnDelta = (deltaString == null) ? null : Duration
                .parse(deltaString);

        final List<RouteSequenceItem> sequence
                = mapSequence(reroute.getSequence(), stopIdMap);
        final RouteReroute routeReroute = new RouteReroute(
                route, referenceStop, type, sequence, returnStop, returnDelta);
        return routeReroute;
    }

    private static ReferenceDirection mapDirection(
            final com.publictransitanalytics.scoregenerator.comparison.ReferenceDirection type) {
        switch (type) {
        case BEFORE_FIRST:
            return ReferenceDirection.BEFORE_FIRST;
        case AFTER_LAST:
            return ReferenceDirection.AFTER_LAST;
        default:
            throw new ScoreGeneratorFatalException(String.format(
                    "Cannot convert extension type %s", type));

        }
    }

    private static List<RouteSequenceItem> mapSequence(
            final List<SequenceItem> sequence,
            final Map<String, TransitStop> stopIdMap) {
        final ImmutableList.Builder<RouteSequenceItem> builder
                = ImmutableList.builder();
        for (final SequenceItem item : sequence) {
            final String deltaString = item.getDelta();
            final String stopId = item.getStopId();
            final Duration delta = Duration.parse(deltaString);
            final TransitStop stop = stopIdMap.get(stopId);
            builder.add(new RouteSequenceItem(delta, stop));
        }
        return builder.build();
    }

    private static ServiceDataDirectory getServiceData(
            final OperationDescription description,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap) {
        final String files = description.getFiles();
        final ServiceDataDirectory serviceDirectory
                = serviceDirectoriesMap.get(files);
        return serviceDirectory;
    }

    private static Set<TransitStop> getAddedStops(
            final OperationDescription description, final Grid grid) {

        final List<ComparisonOperation> operations
                = description.getOperations();
        final ImmutableSet.Builder<TransitStop> builder
                = ImmutableSet.builder();

        if (operations != null) {
            for (final ComparisonOperation operation : operations) {
                switch (operation.getOperator()) {
                case ADD:
                    final Stop stopData = operation.getStop();
                    final GeoPoint location = GeoPoint.parseDegreeString(
                            stopData.getLocation());
                    if (grid.coversPoint(location)) {
                        final String name = stopData.getStopId();

                        final TransitStop newStop = new TransitStop(
                                name, name, location);
                        builder.add(newStop);
                    }
                }
            }
        }
        return builder.build();
    }

    private static Set<TransitStop> getDeletedStops(
            final OperationDescription description,
            final BiMap<String, TransitStop> stopIdMap) {

        final List<ComparisonOperation> operations
                = description.getOperations();
        final ImmutableSet.Builder<TransitStop> builder
                = ImmutableSet.builder();

        if (operations != null) {
            for (final ComparisonOperation operation : operations) {
                switch (operation.getOperator()) {
                case DELETE:
                    final Stop stopData = operation.getStop();
                    if (stopData != null) {
                        builder.add(stopIdMap.get(stopData.getStopId()));
                    }
                }
            }
        }
        return builder.build();
    }

    private static BiMap<String, TransitStop> buildTransitStopIdMap(
            final Grid grid, final StopDetailsDirectory stopDetailsDirectory)
            throws InterruptedException {
        final ImmutableBiMap.Builder<String, TransitStop> stopMapBuilder
                = ImmutableBiMap.builder();
        for (final StopDetails stopDetails : stopDetailsDirectory
                .getAllStopDetails()) {
            final GeoPoint location = new GeoPoint(
                    new GeoLongitude(
                            stopDetails.getCoordinate().getLongitude(),
                            AngleUnit.DEGREES),
                    new GeoLatitude(
                            stopDetails.getCoordinate().getLatitude(),
                            AngleUnit.DEGREES));

            if (grid.coversPoint(location)) {
                final TransitStop stop = new TransitStop(
                        stopDetails.getStopId(), stopDetails.getStopName(),
                        location);
                stopMapBuilder.put(stop.getIdentifier(), stop);
            } else {
                log.debug(
                        "Stop at {} location {} was skipped because it was not in the sector table.",
                        stopDetails, location);
            }
        }
        return stopMapBuilder.build();
    }

    private static <S extends ScoreCard> BiMap<OperationDescription, Calculation<S>> runComparison(
            final OperationDescription baseDescription,
            final ScoreCardFactory scoreCardFactory,
            final Set<Center> centers, final Duration samplingInterval,
            final Duration span, final boolean backward,
            final TimeTracker timeTracker, final Grid grid,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final Duration longestDuration,
            final OperationDescription comparisonDescription,
            final NetworkConsoleFactory consoleFactory)
            throws InterruptedException, IOException, ExecutionException {

        final ImmutableBiMap.Builder<OperationDescription, Calculation<S>> resultBuilder
                = ImmutableBiMap.builder();
        final Calculation<S> calculation = buildCalculation(
                baseDescription, serviceDirectoriesMap, grid, centers,
                longestDuration, backward, span, samplingInterval, timeTracker,
                scoreCardFactory);
        final NetworkConsole console = consoleFactory.getConsole(
                calculation.getTransitNetwork(),
                calculation.getStopIdMap());
        console.enterConsole();

        resultBuilder.put(baseDescription, calculation);

        final Environment environment = new Environment(grid, longestDuration);

        if (comparisonDescription != null) {

            final Calculation trialCalculation = buildCalculation(
                    comparisonDescription, serviceDirectoriesMap, grid, centers,
                    longestDuration, backward, span, samplingInterval,
                    timeTracker,
                    scoreCardFactory);
            final NetworkConsole trialConsole = consoleFactory.getConsole(
                    trialCalculation.getTransitNetwork(),
                    trialCalculation.getStopIdMap());
            trialConsole.enterConsole();

            log.info(
                    "Trial in service time = {}; original in service time = {}.",
                    trialCalculation.getTransitNetwork().getInServiceTime(),
                    calculation.getTransitNetwork().getInServiceTime());
            resultBuilder.put(comparisonDescription, trialCalculation);
        }

        final BiMap<OperationDescription, Calculation<S>> calculations
                = resultBuilder.build();

        final Workflow workflow = new ParallelTaskExecutor(
                new ProgressiveRangeExecutor(
                        new DynamicProgrammingAlgorithm(),
                        environment));

        workflow.calculate(calculations.values());
        return calculations;
    }

    private static <S extends ScoreCard> Calculation<S> buildCalculation(
            final OperationDescription description,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final Grid grid, final Set<Center> centers,
            final Duration longestDuration, final boolean backward,
            final Duration span, final Duration samplingInterval,
            final TimeTracker timeTracker,
            final ScoreCardFactory scoreCardFactory)
            throws InterruptedException {
        final LocalDateTime startTime
                = LocalDateTime.parse(description.getStartTime());
        final ServiceDataDirectory serviceDirectory = getServiceData(
                description, serviceDirectoriesMap);
        final BiMap<String, TransitStop> baseOriginalStopIdMap
                = buildTransitStopIdMap(
                        grid, serviceDirectory.getStopDetailsDirectory());
        final Set<TransitStop> addedStops = getAddedStops(description, grid);
        final BiMap<String, TransitStop> addedStopIdMap = addedStops.stream()
                .collect(ImmutableBiMap.toImmutableBiMap(
                        stop -> stop.getIdentifier(), Function.identity()));
        final BiMap<String, TransitStop> stopIdMap
                = ImmutableBiMap.<String, TransitStop>builder()
                        .putAll(addedStopIdMap)
                        .putAll(baseOriginalStopIdMap).build();
        final List<Patch> basePatches = getTripPatches(description,
                                                       baseOriginalStopIdMap);
        final Set<TransitStop> deletedStops
                = getDeletedStops(description, stopIdMap);

        final Calculation<S> calculation = new Calculation<>(
                grid, centers, longestDuration, backward, span,
                samplingInterval, ESTIMATE_WALK_METERS_PER_SECOND,
                timeTracker, serviceDirectoriesMap, scoreCardFactory, startTime,
                serviceDirectory, basePatches, addedStops, deletedStops,
                stopIdMap);
        return calculation;
    }

    private static void publishNetworkAccessibility(
            final OperationDescription base,
            final OperationDescription comparison,
            final BiMap<OperationDescription, Calculation<ScoreCard>> calculations,
            final Grid grid, final Set<Sector> centerSectors,
            final boolean markCenters, final NavigableSet<Duration> durations,
            final Duration span, final Duration samplingInterval,
            final boolean backward, final FileManager fileManager,
            final Gson serializer, final MapGenerator mapGenerator,
            final String outputName) throws InterruptedException, IOException {
        final Calculation baseCalculation = calculations.get(base);

        final ScoreCard scoreCard = baseCalculation.getScoreCard();
        final Duration inServiceTime = baseCalculation
                .getTransitNetwork().getInServiceTime();
        final int taskCount = scoreCard.getTaskCount();
        final LocalDateTime startTime
                = LocalDateTime.parse(base.getStartTime());
        final LocalDateTime endTime = startTime.plus(span);

        if (comparison == null) {
            final NetworkAccessibility map = new NetworkAccessibility(
                    taskCount, scoreCard, grid, centerSectors,
                    startTime, endTime, durations.last(), samplingInterval,
                    backward, inServiceTime);
            fileManager.publish(outputName, serializer.toJson(map));

            mapGenerator.makeRangeMap(grid, scoreCard, Collections.emptySet(),
                                      0, 0.2, outputName);
        } else {
            for (final OperationDescription trialComparison
                         : calculations.keySet()) {

                final String name = comparison.getName();
                final Calculation trialCalculation
                        = calculations.get(trialComparison);
                final Duration trialInServiceTime = trialCalculation
                        .getTransitNetwork().getInServiceTime();
                final ScoreCard trialScoreCard
                        = trialCalculation.getScoreCard();
                final int trialTaskCount = trialScoreCard.getTaskCount();
                final LocalDateTime trialStartTime
                        = LocalDateTime.parse(comparison.getStartTime());
                final LocalDateTime trialEndTime = startTime.plus(span);

                final ComparativeNetworkAccessibility map
                        = new ComparativeNetworkAccessibility(
                                taskCount, trialTaskCount, scoreCard,
                                trialScoreCard, grid, centerSectors, startTime,
                                endTime, trialStartTime, trialEndTime,
                                durations.last(), samplingInterval,
                                backward, name, inServiceTime,
                                trialInServiceTime);
                fileManager.publish(outputName, serializer.toJson(map));
                mapGenerator.makeComparativeMap(
                        grid, scoreCard, trialScoreCard, Collections.emptySet(),
                        0.2, outputName);
            }
        }
    }

    private static void generatePointAccessibility(
            final Namespace namespace,
            final OperationDescription baseDescription, final boolean backward,
            final Duration samplingInterval, final Duration span,
            final NavigableSet<Duration> durations, final Grid grid,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final OperationDescription comparison,
            final FileManager publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName,
            final NetworkConsoleFactory consoleFactory)
            throws IOException, InterruptedException, ExecutionException {

        final String coordinateString = namespace.get("coordinate");
        final GeoPoint centerCoordinate = (coordinateString == null)
                ? null : GeoPoint.parseDegreeString(coordinateString);
        final Landmark centerPoint = buildCenterPoint(grid, centerCoordinate);

        final MovementAssembler assembler;
        final TimeTracker timeTracker;
        if (!backward) {
            assembler = new ForwardMovementAssembler();
            timeTracker = new ForwardTimeTracker();
        } else {
            assembler = new RetrospectiveMovementAssembler();
            timeTracker = new BackwardTimeTracker();
        }

        final PathScoreCardFactory scoreCardFactory
                = new PathScoreCardFactory(assembler, timeTracker);

        final Center center = new Center(
                centerPoint, Collections.singleton(centerPoint));

        final Map<OperationDescription, Calculation<PathScoreCard>> calculations
                = runComparison(baseDescription, scoreCardFactory,
                                Collections.singleton(center),
                                samplingInterval, span, backward, timeTracker,
                                grid, serviceDirectoriesMap, durations.last(),
                                comparison, consoleFactory);
        final Calculation<PathScoreCard> baseCalculation
                = calculations.get(baseDescription);
        final PathScoreCard scoreCard = baseCalculation.getScoreCard();
        final int taskCount = scoreCard.getTaskCount();

        final LocalDateTime startTime
                = LocalDateTime.parse(baseDescription.getStartTime());
        final String name = baseDescription.getName();
        final Duration inServiceTime = baseCalculation
                .getTransitNetwork().getInServiceTime();

        if (comparison == null) {
            if (span != null) {
                final LocalDateTime endTime = startTime.plus(span);
                final PointAccessibility map = new PointAccessibility(
                        taskCount, scoreCard, grid, centerPoint,
                        startTime, endTime, samplingInterval, durations.last(),
                        backward, inServiceTime);
                publisher.publish(outputName, serializer.toJson(map));
                mapGenerator.makeRangeMap(grid, scoreCard,
                                          Collections.singleton(centerPoint), 0,
                                          1, outputName);
            } else {
                final TimeQualifiedPointAccessibility map
                        = new TimeQualifiedPointAccessibility(
                                scoreCard, grid, centerPoint, startTime,
                                durations.last(), backward, inServiceTime);
                publisher.publish(outputName, serializer.toJson(map));
                mapGenerator.makeRangeMap(grid, scoreCard,
                                          Collections.singleton(centerPoint), 0,
                                          1, outputName);
            }

        } else {
            for (final OperationDescription trialComparison
                         : calculations.keySet()) {
                final String trialName = comparison.getName();
                final Calculation<PathScoreCard> trialCalculation
                        = calculations.get(trialComparison);
                final PathScoreCard trialScoreCard
                        = trialCalculation.getScoreCard();
                final int trialTaskCount = trialScoreCard.getTaskCount();
                final LocalDateTime trialStartTime = LocalDateTime.parse(
                        comparison.getStartTime());
                final Duration trialInServiceTime = trialCalculation
                        .getTransitNetwork().getInServiceTime();

                if (span != null) {
                    final LocalDateTime endTime = startTime.plus(span);
                    final LocalDateTime trialEndTime
                            = trialStartTime.plus(span);
                    final ComparativePointAccessibility map
                            = new ComparativePointAccessibility(
                                    taskCount, trialTaskCount, scoreCard,
                                    trialScoreCard, grid,
                                    centerPoint, startTime, endTime,
                                    trialStartTime, trialEndTime,
                                    samplingInterval, durations.last(),
                                    backward, trialName, inServiceTime,
                                    trialInServiceTime);
                    publisher.publish(outputName, serializer.toJson(map));
                } else {
                    final ComparativeTimeQualifiedPointAccessibility map
                            = new ComparativeTimeQualifiedPointAccessibility(
                                    scoreCard, trialScoreCard, grid,
                                    centerPoint, startTime, trialStartTime,
                                    durations.last(), backward, name,
                                    trialName, inServiceTime,
                                    trialInServiceTime);
                    publisher.publish(outputName, serializer.toJson(map));
                }
                mapGenerator.makeComparativeMap(
                        grid, scoreCard, trialScoreCard,
                        Collections.singleton(centerPoint), 0.2, outputName);
            }
        }
    }

    private static Landmark buildCenterPoint(
            final Grid grid, final GeoPoint centerCoordinate) {

        if (!grid.coversPoint(centerCoordinate)) {
            throw new ScoreGeneratorFatalException(String.format(
                    "Starting location %s was not in the SectorTable",
                    centerCoordinate));
        }
        final Landmark centerPoint = new Landmark(centerCoordinate);
        return centerPoint;
    }

    private static GeoBounds parseBounds(final String boundsString) {
        final String boundsStrings[] = boundsString.split(",");
        final GeoBounds bounds = new GeoBounds(
                new GeoLongitude(boundsStrings[0], AngleUnit.DEGREES),
                new GeoLatitude(boundsStrings[1], AngleUnit.DEGREES),
                new GeoLongitude(boundsStrings[2], AngleUnit.DEGREES),
                new GeoLatitude(boundsStrings[3], AngleUnit.DEGREES));
        return bounds;
    }

    private static Grid getGrid(final Path root, final GeoBounds bounds,
                                final StoreFactory storeFactory)
            throws IOException, InEnvironmentDetectorException,
            InterruptedException {
        final Path osmPath = root.resolve(OSM_FILE);

        final Path borderFilePath = root.resolve(BORDER_FILE);
        final Path waterFilePath = root.resolve(WATER_BODIES_FILE);

        final GeoJsonInEnvironmentDetector detector
                = new GeoJsonInEnvironmentDetector(borderFilePath,
                                                   waterFilePath);

        final ReadingSegmentFinder segmentFinder = new ReadingSegmentFinder(
                osmPath, bounds);

        final Serializer<GridInfo> gridInfoSerializer
                = new GsonSerializer<>(GridInfo.class);
        final Store<GridIdKey, GridInfo> gridInfoStore = storeFactory.getStore(
                root.resolve(GRID_INFO_STORE), gridInfoSerializer);

        final Serializer<SectorInfo> sectorInfoSerializer
                = new GsonSerializer<>(SectorInfo.class
                );
        final RangedStore<SectorKey, SectorInfo> sectorStore
                = storeFactory.<SectorKey, SectorInfo>getRangedStore(
                        root.resolve(SECTOR_INFO_STORE),
                        new SectorKey.Materializer(), sectorInfoSerializer);

        final Serializer<GridPointAssociation> gridPointAssociationSerializer
                = new GsonSerializer<>(GridPointAssociation.class
                );
        final RangedStore<GridPointAssociationKey, GridPointAssociation> assocationStore
                = storeFactory
                        .<GridPointAssociationKey, GridPointAssociation>getRangedStore(
                                root.resolve(GRID_POINT_ASSOCATION_STORE),
                                new GridPointAssociationKey.Materializer(),
                                gridPointAssociationSerializer);

        return new StoredGrid(segmentFinder, bounds, RESOLUTION_METERS,
                              detector, gridInfoStore, sectorStore,
                              assocationStore);
    }

}
