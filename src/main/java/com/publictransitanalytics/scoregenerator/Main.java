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

import com.publictransitanalytics.scoregenerator.console.NetworkConsoleFactory;
import com.publictransitanalytics.scoregenerator.console.DummyNetworkConsole;
import com.publictransitanalytics.scoregenerator.console.InteractiveNetworkConsole;
import com.publictransitanalytics.scoregenerator.console.NetworkConsole;
import com.publictransitanalytics.scoregenerator.comparison.OperationDescription;
import com.publictransitanalytics.scoregenerator.workflow.Environment;
import com.publictransitanalytics.scoregenerator.workflow.Calculation;
import com.google.common.collect.BiMap;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
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
import com.publictransitanalytics.scoregenerator.datalayer.directories.EnvironmentDataDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceDataDirectory;
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.environment.Segment;
import com.publictransitanalytics.scoregenerator.environment.SegmentFinder;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetectorException;
import com.publictransitanalytics.scoregenerator.output.ComparativeNetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.ComparativePointAccessibility;
import com.publictransitanalytics.scoregenerator.output.ComparativeTimeQualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.output.MapGenerator;
import com.publictransitanalytics.scoregenerator.output.NetworkAccessibility;
import com.publictransitanalytics.scoregenerator.output.PointAccessibility;
import com.publictransitanalytics.scoregenerator.output.TimeQualifiedPointAccessibility;
import com.publictransitanalytics.scoregenerator.publishing.LocalFilePublisher;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.workflow.ProgressiveRangeExecutor;
import java.util.ArrayList;
import java.util.Collections;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import com.publictransitanalytics.scoregenerator.workflow.Workflow;
import java.util.Map;
import com.publictransitanalytics.scoregenerator.scoring.CountScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.CountScoreCardFactory;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCardFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingAlgorithm;
import com.publictransitanalytics.scoregenerator.workflow.ForwardMovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.MovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.NullMovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.ParallelTaskExecutor;
import com.publictransitanalytics.scoregenerator.workflow.RetrospectiveMovementAssembler;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

@Slf4j
/**
 * Parses inputs, builds up infrastructure, and delegates to solve reachability
 * problems.
 *
 * @author Public Transit Analytics
 */
public class Main {

    private static final GeoBounds SEATTLE_BOUNDS = new GeoBounds(
            new GeoLongitude("-122.45970", AngleUnit.DEGREES),           
            new GeoLatitude("47.48172", AngleUnit.DEGREES),
            new GeoLongitude("-122.22443", AngleUnit.DEGREES),
            new GeoLatitude("47.734145", AngleUnit.DEGREES));

    private static final int NUM_LATITUDE_SECTORS = 100;
    private static final int NUM_LONGITUDE_SECTORS = 100;

    private static final double ESTIMATE_WALK_METERS_PER_SECOND = 2.0;

    public static void main(String[] args) throws FileNotFoundException,
            IOException, ArgumentParserException, InterruptedException,
            ExecutionException, WaterDetectorException {
        final Gson serializer = new GsonBuilder().setPrettyPrinting().create();

        final ArgumentParser parser = ArgumentParsers.newArgumentParser(
                "ScoreGenerator").defaultHelp(true)
                .description("Generate isochrone map data.");

        parser.addArgument("-l", "--tripLengths").action(Arguments.append());
        parser.addArgument("-i", "--samplingInterval");
        parser.addArgument("-s", "--span");
        parser.addArgument("-d", "--baseDirectory");
        parser.addArgument("-k", "--backward").action(Arguments.storeTrue());
        parser.addArgument("-n", "--inMemCache").action(Arguments.storeTrue());
        parser.addArgument("-t", "--interactive").action(Arguments.storeTrue());
        parser.addArgument("-b", "--baseFile");
        parser.addArgument("-c", "--comparisonFile");
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

        final String baseDirectoryString = namespace.get("baseDirectory");
        final Path root = Paths.get(baseDirectoryString);

        final String baseFile = namespace.get("baseFile");
        final OperationDescription baseDescription = serializer.fromJson(
                new String(Files.readAllBytes(Paths.get(baseFile)),
                           StandardCharsets.UTF_8), OperationDescription.class);

        final String comparisonFile = namespace.get("comparisonFile");
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

        final LocalFilePublisher publisher = new LocalFilePublisher();

        final EnvironmentDataDirectory environmentDirectory
                = new EnvironmentDataDirectory(root);

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
                final ServiceDataDirectory directory = new ServiceDataDirectory(
                        root, fileName, storeFactory);
                serviceDirectoriesMap.put(fileName, directory);
            }
        }

        final File osmFile = environmentDirectory.getOsmPath().toFile();
        final SegmentFinder segmentFinder = new SegmentFinder(
                osmFile, SEATTLE_BOUNDS);

        final WaterDetector waterDetector
                = environmentDirectory.getWaterDetector();
        final Set<Segment> segments = segmentFinder.getSegments();
        final Grid grid = new Grid(segments, SEATTLE_BOUNDS, 100, 100,
                                   waterDetector);

        final MapGenerator mapGenerator = new MapGenerator();

        if ("generatePointAccessibility".equals(command)) {

            generatePointAccessibility(
                    namespace, baseDescription, grid,
                    backward, samplingInterval, span, durations,
                    environmentDirectory, serviceDirectoriesMap,
                    comparisonDescription, publisher,
                    serializer, mapGenerator, outputName, consoleFactory);
        } else if ("generateNetworkAccessibility".equals(command)) {
            final ScoreCardFactory scoreCardFactory
                    = new CountScoreCardFactory();

            generateNetworkAccessibility(
                    baseDescription, scoreCardFactory, grid, backward,
                    samplingInterval, span, durations, environmentDirectory,
                    serviceDirectoriesMap,
                    comparisonDescription, publisher, serializer, mapGenerator,
                    outputName, consoleFactory);
        } else if ("generateSampledNetworkAccessibility".equals(command)) {
            final ScoreCardFactory scoreCardFactory
                    = new CountScoreCardFactory();

            generateSampledNetworkAccessibility(
                    namespace, baseDescription, scoreCardFactory, grid,
                    backward, samplingInterval, span, durations,
                    environmentDirectory, serviceDirectoriesMap,
                    comparisonDescription, publisher,
                    serializer, mapGenerator, outputName, consoleFactory);
        }

    }

    private static void generateNetworkAccessibility(
            final OperationDescription base,
            final ScoreCardFactory scoreCardFactory,
            final Grid grid, final boolean backward,
            final Duration samplingInterval, final Duration span,
            final NavigableSet<Duration> durations,
            final EnvironmentDataDirectory environmentDirectory,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final OperationDescription comparison,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName,
            final NetworkConsoleFactory consoleFactory)
            throws IOException, InterruptedException, ExecutionException {

        final Set<PointLocation> centerPoints = grid.getCenters();
        final MovementAssembler assembler = new NullMovementAssembler();
        final BiMap<OperationDescription, Calculation<CountScoreCard>> result
                = calculate(base, scoreCardFactory, assembler, grid,
                            centerPoints, samplingInterval, span, backward,
                            environmentDirectory,
                            serviceDirectoriesMap, durations.last(),
                            comparison, consoleFactory);
        publishNetworkAccessibility(base, comparison, result, grid,
                                    centerPoints, false, durations, span,
                                    samplingInterval, backward, publisher,
                                    serializer, mapGenerator, outputName);
    }

    private static <S extends ScoreCard> BiMap<OperationDescription, Calculation<S>> calculate(
            final OperationDescription baseDescription,
            final ScoreCardFactory<S> scoreCardFactory,
            final MovementAssembler assembler, final Grid grid,
            final Set<PointLocation> centers, final Duration samplingInterval,
            final Duration span, final boolean backward,
            final EnvironmentDataDirectory environmentDirectory,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final Duration longestDuration,
            final OperationDescription comparisonDescription,
            final NetworkConsoleFactory consoleFactory)
            throws InterruptedException, IOException, ExecutionException {

        final ImmutableBiMap.Builder<OperationDescription, Calculation<S>> resultBuilder
                = ImmutableBiMap.builder();
        final Calculation<S> calculation = new Calculation(
                grid, centers, longestDuration, backward, span,
                samplingInterval, ESTIMATE_WALK_METERS_PER_SECOND,
                environmentDirectory, serviceDirectoriesMap,
                scoreCardFactory, baseDescription);
        final NetworkConsole console = consoleFactory.getConsole(
                calculation.getTransitNetwork(),
                calculation.getStopIdMap());
        console.enterConsole();

        resultBuilder.put(baseDescription, calculation);

        final Environment environment = new Environment(grid, longestDuration);

        if (comparisonDescription != null) {

            final Calculation trialCalculation = new Calculation(
                    grid, centers, longestDuration, backward, span,
                    samplingInterval, ESTIMATE_WALK_METERS_PER_SECOND,
                    environmentDirectory, serviceDirectoriesMap,
                    scoreCardFactory, comparisonDescription);
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

    private static void generateSampledNetworkAccessibility(
            final Namespace namespace,
            final OperationDescription baseDescription,
            final ScoreCardFactory scoreCardFactory,
            final Grid grid, final boolean backward,
            final Duration samplingInterval, final Duration span,
            final NavigableSet<Duration> durations,
            final EnvironmentDataDirectory environmentDirectory,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final OperationDescription comparison,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName,
            final NetworkConsoleFactory consoleFactory)
            throws IOException, InterruptedException, ExecutionException {

        final int samples = Integer.valueOf(namespace.get("samples"));

        final List<PointLocation> centerList = new ArrayList(grid.getCenters());
        Collections.shuffle(centerList);
        final ImmutableSet<PointLocation> centerPoints
                = ImmutableSet.copyOf(centerList.subList(0, samples));

        final MovementAssembler assembler = new NullMovementAssembler();

        final BiMap<OperationDescription, Calculation<CountScoreCard>> result
                = calculate(baseDescription, scoreCardFactory, assembler,
                            grid, centerPoints, samplingInterval, span,
                            backward, environmentDirectory,
                            serviceDirectoriesMap, durations.last(),
                            comparison, consoleFactory);
        publishNetworkAccessibility(baseDescription, comparison, result,
                                    grid, centerPoints, true, durations,
                                    span, samplingInterval, backward, publisher,
                                    serializer, mapGenerator, outputName);
    }

    private static void publishNetworkAccessibility(
            final OperationDescription base,
            final OperationDescription comparison,
            final BiMap<OperationDescription, Calculation<CountScoreCard>> calculations,
            final Grid grid, final Set<PointLocation> centerPoints,
            final boolean markCenters, final NavigableSet<Duration> durations,
            final Duration span, final Duration samplingInterval,
            final boolean backward, final LocalFilePublisher publisher,
            final Gson serializer, final MapGenerator mapGenerator,
            final String outputName) throws InterruptedException, IOException {
        final Calculation baseCalculation = calculations.get(base);

        final ScoreCard scoreCard = baseCalculation.getScoreCard();
        final Duration inServiceTime = baseCalculation
                .getTransitNetwork().getInServiceTime();
        final int taskCount = baseCalculation.getTaskCount();
        final LocalDateTime startTime
                = LocalDateTime.parse(base.getStartTime());
        final LocalDateTime endTime = startTime.plus(span);
        final Set<PointLocation> markedPoints = markCenters
                ? centerPoints : Collections.emptySet();
        if (comparison == null) {
            final NetworkAccessibility map = new NetworkAccessibility(
                    taskCount, scoreCard, grid, centerPoints,
                    startTime, endTime, durations.last(), samplingInterval,
                    backward, inServiceTime);
            publisher.publish(outputName, serializer.toJson(map));

            mapGenerator.makeRangeMap(grid, scoreCard, markedPoints,
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
                final int trialTaskCount = trialCalculation.getTaskCount();
                final LocalDateTime trialStartTime
                        = LocalDateTime.parse(comparison.getStartTime());
                final LocalDateTime trialEndTime = startTime.plus(span);

                final ComparativeNetworkAccessibility map
                        = new ComparativeNetworkAccessibility(
                                taskCount, trialTaskCount, scoreCard,
                                trialScoreCard, grid, centerPoints, startTime,
                                endTime, trialStartTime, trialEndTime,
                                durations.last(), samplingInterval,
                                backward, name, inServiceTime,
                                trialInServiceTime);
                publisher.publish(outputName, serializer.toJson(map));
                mapGenerator.makeComparativeMap(
                        grid, scoreCard, trialScoreCard, markedPoints, 0.2,
                        outputName);

            }
        }
    }

    private static void generatePointAccessibility(
            final Namespace namespace,
            final OperationDescription baseDescription,
            final Grid grid, final boolean backward,
            final Duration samplingInterval, final Duration span,
            final NavigableSet<Duration> durations,
            final EnvironmentDataDirectory environmentDirectory,
            final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
            final OperationDescription comparison,
            final LocalFilePublisher publisher, final Gson serializer,
            final MapGenerator mapGenerator, final String outputName,
            final NetworkConsoleFactory consoleFactory)
            throws IOException, InterruptedException, ExecutionException {

        final String coordinateString = namespace.get("coordinate");
        final GeoPoint centerCoordinate = (coordinateString == null)
                ? null : GeoPoint.parseDegreeString(coordinateString);
        final Landmark centerPoint = buildCenterPoint(grid, centerCoordinate);

        final MovementAssembler assembler;
        if (!backward) {
            assembler = new ForwardMovementAssembler();

        } else {
            assembler = new RetrospectiveMovementAssembler();
        }

        final PathScoreCardFactory scoreCardFactory
                = new PathScoreCardFactory(assembler);

        final Map<OperationDescription, Calculation<PathScoreCard>> calculations
                = calculate(baseDescription, scoreCardFactory, assembler,
                            grid, Collections.singleton(centerPoint),
                            samplingInterval, span, backward,
                            environmentDirectory, serviceDirectoriesMap,
                            durations.last(), comparison, consoleFactory);
        final Calculation<PathScoreCard> baseCalculation
                = calculations.get(baseDescription);
        final PathScoreCard scoreCard = baseCalculation.getScoreCard();
        final int taskCount = baseCalculation.getTaskCount();

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
                                scoreCard, grid, centerPoint,
                                startTime, durations.last(), backward,
                                inServiceTime);
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
                final int trialTaskCount = trialCalculation.getTaskCount();
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

}
