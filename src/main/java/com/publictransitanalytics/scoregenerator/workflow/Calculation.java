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
package com.publictransitanalytics.scoregenerator.workflow;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.comparison.ComparisonOperation;
import com.publictransitanalytics.scoregenerator.comparison.Extension;
import com.publictransitanalytics.scoregenerator.comparison.OperationDescription;
import com.publictransitanalytics.scoregenerator.comparison.Reroute;
import com.publictransitanalytics.scoregenerator.comparison.SequenceItem;
import com.publictransitanalytics.scoregenerator.comparison.Stop;
import com.publictransitanalytics.scoregenerator.schedule.patching.Transformer;
import com.publictransitanalytics.scoregenerator.comparison.Truncation;
import com.publictransitanalytics.scoregenerator.datalayer.directories.EnvironmentDataDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceDataDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.StopDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.StopDetails;
import com.publictransitanalytics.scoregenerator.distanceclient.BackwardPointOrderer;
import com.publictransitanalytics.scoregenerator.distanceclient.CachingDistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.CompletePairGenerator;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateRefiningReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.EstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.ForwardPointOrderer;
import com.publictransitanalytics.scoregenerator.distanceclient.GraphhopperLocalDistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.PairGenerator;
import com.publictransitanalytics.scoregenerator.distanceclient.PermanentEstimateStorage;
import com.publictransitanalytics.scoregenerator.distanceclient.PointOrdererFactory;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.StoredDistanceEstimator;
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingTripCreator;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.LocalDateTime;
import java.util.Set;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripProcessingTransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteDeletion;
import com.publictransitanalytics.scoregenerator.schedule.patching.ReferenceDirection;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteExtension;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteReroute;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteSequenceItem;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteTruncation;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import com.publictransitanalytics.scoregenerator.walking.BackwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class Calculation<S extends ScoreCard> {

    @Getter
    private final Set<TaskGroupIdentifier> taskGroups;
    @Getter
    private final NavigableSet<LocalDateTime> times;
    @Getter
    private final S scoreCard;
    @Getter
    private final TimeTracker timeTracker;
    private final Set<ModeType> allowedModes;
    @Getter
    private final TransitNetwork transitNetwork;
    private final boolean backward;
    private final Duration longestDuration;
    private final double walkingMetersPerSecond;
    private final DistanceClient distanceClient;
    @Getter
    private final BiMap<String, TransitStop> stopIdMap;
    @Getter
    private final SetMultimap<PointLocation, Sector> pointSectorMap;
    private final Set<GridPoint> gridPoints;
    private final DistanceEstimator distanceEstimator;
    @Getter
    private final ReachabilityClient reachabilityClient;
    @Getter
    private final RiderFactory riderFactory;

    public Calculation(final Grid grid, final Set<PointLocation> centers,
                       final Duration longestDuration, final boolean backward,
                       final Duration span, final Duration samplingInterval,
                       final double walkingMetersPerSecond,
                       final EnvironmentDataDirectory environmentDirectory,
                       final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
                       final ScoreCardFactory<S> scoreCardFactory,
                       final OperationDescription description)
            throws InterruptedException, IOException {

        final String files = description.getFiles();
        final LocalDateTime startTime
                = LocalDateTime.parse(description.getStartTime());
        final LocalDateTime endTime = (span != null)
                ? startTime.plus(span) : null;

        final ServiceDataDirectory serviceDirectory
                = serviceDirectoriesMap.get(files);

        this.walkingMetersPerSecond = walkingMetersPerSecond;
        this.longestDuration = longestDuration;
        this.backward = backward;
        gridPoints = grid.getGridPoints();

        final BiMap<String, TransitStop> baseStopIdMap = buildTransitStopIdMap(
                grid, serviceDirectory.getStopDetailsDirectory());

        final LocalDateTime earliestTime = getEarliestTime(
                startTime, longestDuration, backward);
        final LocalDateTime latestTime = getLatestTime(
                startTime, endTime, longestDuration, backward);

        final TransitNetwork baseTransitNetwork = buildTransitNetwork(
                serviceDirectory, earliestTime, latestTime, baseStopIdMap);

        final SetMultimap<PointLocation, Sector> basePointSectorMap
                = buildPointSectorMap(grid, baseStopIdMap.values());

        final PointOrdererFactory ordererFactory;
        final RiderFactory baseRiderFactory;
        if (!backward) {
            baseRiderFactory = new ForwardRiderFactory(baseTransitNetwork);
            timeTracker = new ForwardTimeTracker();
            ordererFactory = (point, consideredPoint)
                    -> new ForwardPointOrderer(point, consideredPoint);
            distanceClient = buildDistanceClient(
                    environmentDirectory, serviceDirectory, ordererFactory);

        } else {
            baseRiderFactory = new RetrospectiveRiderFactory(
                    baseTransitNetwork);
            timeTracker = new BackwardTimeTracker();
            ordererFactory = (point, consideredPoint)
                    -> new BackwardPointOrderer(point, consideredPoint);
            distanceClient = buildDistanceClient(
                    environmentDirectory, serviceDirectory, ordererFactory);
        }

        final BiMap<String, PointLocation> basePointIdMap = buildPointIdMap(
                centers, baseStopIdMap, grid);

        final DistanceEstimator baseDistanceEstimator = buildDistanceEstimator(
                serviceDirectory, grid.getGridPoints(), centers,
                baseStopIdMap.values(), longestDuration, basePointIdMap,
                walkingMetersPerSecond);

        taskGroups = getTaskGroups(centers);
        times = getTaskTimes(startTime, endTime, samplingInterval);

        final ReachabilityClient baseReachabilityClient
                = buildReachabilityClient(distanceClient, baseDistanceEstimator,
                                          timeTracker, walkingMetersPerSecond);

        allowedModes = ImmutableSet.of(ModeType.TRANSIT, ModeType.WALKING);

        stopIdMap = HashBiMap.create(baseStopIdMap);
        pointSectorMap = HashMultimap.create(basePointSectorMap);

        final List<ComparisonOperation> operations
                = description.getOperations();

        final Transformer transformer = new Transformer(
                timeTracker, baseTransitNetwork, backward, longestDuration,
                walkingMetersPerSecond, distanceClient, gridPoints,
                baseStopIdMap.values(), centers, baseDistanceEstimator,
                baseReachabilityClient, baseRiderFactory);

        if (operations != null) {
            for (final ComparisonOperation operation : operations) {
                final String route = operation.getRoute();

                switch (operation.getOperator()) {
                case DELETE:
                    final RouteDeletion deletion = new RouteDeletion(route);
                    transformer.addTripPatch(deletion);
                    break;
                case ADD:
                    final Stop stopData = operation.getStop();
                    final GeoPoint location = GeoPoint.parseDegreeString(
                            stopData.getLocation());
                    if (grid.coversPoint(location)) {
                        final String name = stopData.getStopId();

                        final TransitStop newStop = new TransitStop(
                                name, name, location);

                        stopIdMap.put(name, newStop);
                        pointSectorMap.putAll(newStop,
                                              grid.getSectors(newStop));
                        transformer.addStop(newStop);
                    }
                    break;
                case EXTEND:
                    final Extension extension = operation.getExtension();

                    final RouteExtension routeExtension = getRouteExtension(
                            route, extension, stopIdMap);
                    transformer.addTripPatch(routeExtension);
                    break;
                case TRUNCATE:
                    final Truncation truncation = operation.getTruncation();

                    final RouteTruncation routeTruncation = getRouteTruncation(
                            route, truncation, stopIdMap);
                    transformer.addTripPatch(routeTruncation);
                    break;
                case REROUTE:
                    final Reroute reroute = operation.getReroute();
                    final RouteReroute routeReroute = getRouteReroute(
                            route, reroute, stopIdMap);
                    transformer.addTripPatch(routeReroute);
                    break;
                }
            }
        }

        scoreCard = scoreCardFactory.makeScoreCard(
                times.size() * taskGroups.size(), pointSectorMap);

        transitNetwork = transformer.getTransitNetwork();
        riderFactory = transformer.getRiderFactory();
        distanceEstimator = transformer.getDistanceEstimator();
        reachabilityClient = transformer.getReachabilityClient();
    }

    private static DistanceClient buildDistanceClient(
            final EnvironmentDataDirectory environmentDirectory,
            final ServiceDataDirectory dataDirectory,
            final PointOrdererFactory ordererFactory) {

        final GraphhopperLocalDistanceClient graphhopperDistanceClient
                = new GraphhopperLocalDistanceClient(
                        ordererFactory, environmentDirectory.getHopper(),
                        environmentDirectory.getWaterDetector());
        final DistanceClient distanceClient = new CachingDistanceClient(
                ordererFactory, dataDirectory.getWalkingDistanceStore(),
                graphhopperDistanceClient);

        return distanceClient;
    }

    public int getTaskCount() {
        return taskGroups.size() * times.size();
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

    private static DistanceEstimator buildDistanceEstimator(
            final ServiceDataDirectory serviceDirectory,
            final Set<GridPoint> gridPoints,
            final Set<PointLocation> centers,
            final Set<TransitStop> stops,
            final Duration maxDuration,
            final BiMap<String, PointLocation> locationIdMap,
            final double walkingDistanceMetersPerSecond)
            throws InterruptedException {

        final double maxWalkingDistance
                = walkingDistanceMetersPerSecond * maxDuration.getSeconds();
        final EstimateStorage estimateStorage = new PermanentEstimateStorage(
                serviceDirectory.getMaxCandidateDistanceStore(),
                serviceDirectory.getCandidateDistancesStore(), locationIdMap);
        final PairGenerator pairGenerator = new CompletePairGenerator(
                gridPoints, stops, centers);

        final DistanceEstimator distanceEstimator = new StoredDistanceEstimator(
                pairGenerator, maxWalkingDistance, estimateStorage);
        return distanceEstimator;
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

    private static BiMap<String, PointLocation> buildPointIdMap(
            final Set<PointLocation> centerPoints,
            final BiMap<String, TransitStop> transitStops, final Grid grid) {
        final ImmutableBiMap.Builder<String, PointLocation> pointMapBuilder
                = ImmutableBiMap.builder();
        pointMapBuilder.putAll(transitStops);

        for (final PointLocation centerPoint : centerPoints) {
            pointMapBuilder.put(centerPoint.getIdentifier(), centerPoint);
        }
        for (final PointLocation gridPoint : grid.getGridPoints()) {
            pointMapBuilder.put(gridPoint.getIdentifier(), gridPoint);
        }
        final BiMap<String, PointLocation> pointIdMap
                = pointMapBuilder.build();
        return pointIdMap;
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

    private static TransitNetwork buildTransitNetwork(
            final ServiceDataDirectory serviceData,
            final LocalDateTime earliestTime, final LocalDateTime latestTime,
            final BiMap<String, TransitStop> stopIdMap)
            throws IOException, InterruptedException {

        final TransitNetwork transitNetwork
                = new TripProcessingTransitNetwork(
                        new DirectoryReadingTripCreator(
                                earliestTime, latestTime,
                                serviceData.getStopTimesDirectory(),
                                serviceData.getRouteDetailsDirectory(),
                                serviceData.getTripDetailsDirectory(),
                                serviceData.getServiceTypeCalendar(),
                                stopIdMap));
        return transitNetwork;
    }

    private static ReachabilityClient buildReachabilityClient(
            final DistanceClient distanceClient,
            final DistanceEstimator distanceEstimator,
            final TimeTracker timeTracker,
            final double walkMetersPerSecond) {
        final ReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        distanceClient, distanceEstimator, timeTracker,
                        walkMetersPerSecond);
        return reachabilityClient;
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

    private static SetMultimap<PointLocation, Sector> buildPointSectorMap(
            final Grid grid, final Set<TransitStop> stops) {
        final ImmutableSetMultimap.Builder<PointLocation, Sector> builder
                = ImmutableSetMultimap.builder();

        for (final PointLocation gridPoint : grid.getGridPoints()) {
            builder.putAll(gridPoint, grid.getSectors(gridPoint));
        }
        for (final TransitStop stop : stops) {
            if (grid.coversPoint(stop.getLocation())) {
                final Set<Sector> sectors = grid.getSectors(stop);
                if (sectors == null) {
                    log.warn("{} ({}) is not on a land Sector.",
                             stop.getCommonName(), stop.getLocation());
                } else {
                    builder.putAll(stop, sectors);
                }
            }
        }
        return builder.build();
    }

}
