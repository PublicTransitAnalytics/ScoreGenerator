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
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.comparison.ComparisonOperation;
import com.publictransitanalytics.scoregenerator.comparison.Extension;
import com.publictransitanalytics.scoregenerator.comparison.OperationDescription;
import com.publictransitanalytics.scoregenerator.comparison.Stop;
import com.publictransitanalytics.scoregenerator.comparison.Transformer;
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
import com.publictransitanalytics.scoregenerator.geography.EndpointDeterminer;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingTripCreator;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Value;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripProcessingTransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.patching.Deletion;
import com.publictransitanalytics.scoregenerator.schedule.patching.ExtensionType;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteExtension;
import com.publictransitanalytics.scoregenerator.schedule.patching.RouteTruncation;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import com.publictransitanalytics.scoregenerator.walking.BackwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
@Value
@Slf4j
public class Calculation<S extends ScoreCard> {

    private final Set<TaskGroupIdentifier> taskGroups;
    private final NavigableSet<LocalDateTime> times;
    private final S scoreCard;
    private final TimeTracker timeTracker;
    private final MovementAssembler movementAssembler;
    private final Set<ModeType> allowedModes;
    private final TransitNetwork transitNetwork;
    private final boolean backward;
    private final Duration longestDuration;
    private final double walkingMetersPerSecond;
    private final EndpointDeterminer endpointDeterminer;
    private final DistanceClient distanceClient;
    private final Set<Sector> sectors;
    private final BiMap<String, TransitStop> stopIdMap;
    private final Set<PointLocation> centers;
    private final DistanceEstimator distanceEstimator;
    private final ReachabilityClient reachabilityClient;
    private final RiderFactory riderFactory;

    public Calculation(final SectorTable sectorTable,
                       final Set<PointLocation> centers,
                       final Duration longestDuration, final boolean backward,
                       final Duration span, final Duration samplingInterval,
                       final double walkingMetersPerSecond,
                       final EndpointDeterminer endpointDeterminer,
                       final EnvironmentDataDirectory environmentDirectory,
                       final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
                       final ScoreCardFactory<S> scoreCardFactory,
                       final MovementAssembler movementAssembler,
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
        this.movementAssembler = movementAssembler;
        this.endpointDeterminer = endpointDeterminer;
        this.centers = centers;

        sectors = sectorTable.getSectors();

        stopIdMap = buildTransitStopIdMap(
                sectorTable, serviceDirectory.getStopDetailsDirectory());

        final BiMap<String, PointLocation> pointIdMap
                = buildPointIdMap(centers, stopIdMap);
        final BiMap<String, VisitableLocation> locationIdMap
                = buildLocationIdMap(pointIdMap, sectorTable);

        final LocalDateTime earliestTime = getEarliestTime(
                startTime, longestDuration, backward);
        final LocalDateTime latestTime = getLatestTime(
                startTime, endTime, longestDuration, backward);

        final TransitNetwork baseTransitNetwork = buildTransitNetwork(
                serviceDirectory, earliestTime, latestTime, stopIdMap);

        final PointOrdererFactory ordererFactory;
        final RiderFactory baseRiderFactory;
        if (!backward) {
            baseRiderFactory = new ForwardRiderFactory(baseTransitNetwork);
            timeTracker = new ForwardTimeTracker();
            ordererFactory = (point, consideredPoint)
                    -> new ForwardPointOrderer(point, consideredPoint);
            distanceClient = buildDistanceClient(
                    environmentDirectory, serviceDirectory,
                    endpointDeterminer, ordererFactory);

        } else {
            baseRiderFactory = new RetrospectiveRiderFactory(
                    baseTransitNetwork);
            timeTracker = new BackwardTimeTracker();
            ordererFactory = (point, consideredPoint)
                    -> new BackwardPointOrderer(point, consideredPoint);
            distanceClient = buildDistanceClient(
                    environmentDirectory, serviceDirectory,
                    endpointDeterminer, ordererFactory);
        }

        final DistanceEstimator baseDistanceEstimator = buildDistanceEstimator(
                serviceDirectory, sectorTable, endpointDeterminer, centers,
                stopIdMap, longestDuration, locationIdMap,
                walkingMetersPerSecond);

        taskGroups = getTaskGroups(centers);
        times = getTaskTimes(startTime, endTime, samplingInterval);

        final ReachabilityClient baseReachabilityClient
                = buildReachabilityClient(distanceClient, baseDistanceEstimator,
                                          timeTracker, walkingMetersPerSecond);
        scoreCard = scoreCardFactory.makeScoreCard(
                times.size() * taskGroups.size());
        allowedModes = ImmutableSet.of(ModeType.TRANSIT, ModeType.WALKING);

        final Map<String, TransitStop> allStopsById = new HashMap<>();
        allStopsById.putAll(stopIdMap);
        final List<ComparisonOperation> operations
                = description.getOperations();

        final Transformer transformer = new Transformer(
                timeTracker, baseTransitNetwork, backward, longestDuration,
                walkingMetersPerSecond, endpointDeterminer, distanceClient,
                sectors, stopIdMap.values(), centers, baseDistanceEstimator,
                baseReachabilityClient, baseRiderFactory);

        if (operations != null) {
            for (final ComparisonOperation operation : operations) {
                final String route = operation.getRoute();

                switch (operation.getOperator()) {
                case DELETE:
                    final Deletion deletion = new Deletion(route);
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
        transitNetwork = transformer.getTransitNetwork();
        riderFactory = transformer.getRiderFactory();
        distanceEstimator = transformer.getDistanceEstimator();
        reachabilityClient = transformer.getReachabilityClient();
    }

    private static DistanceClient buildDistanceClient(
            final EnvironmentDataDirectory environmentDirectory,
            final ServiceDataDirectory dataDirectory,
            final EndpointDeterminer endpointDeterminer,
            final PointOrdererFactory ordererFactory) {

        final GraphhopperLocalDistanceClient graphhopperDistanceClient
                = new GraphhopperLocalDistanceClient(
                        ordererFactory, environmentDirectory.getHopper(),
                        endpointDeterminer,
                        environmentDirectory.getWaterDetector());
        final DistanceClient distanceClient = new CachingDistanceClient(
                ordererFactory, dataDirectory.getWalkingDistanceStore(),
                graphhopperDistanceClient);

        return distanceClient;
    }

    public int getTaskCount() {
        return taskGroups.size() * times.size();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(final Object other) {
        return other == this;
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
            final SectorTable sectorTable,
            final EndpointDeterminer endpointDeterminer,
            final Set<PointLocation> centers,
            final BiMap<String, TransitStop> stopIdMap,
            final Duration maxDuration,
            final BiMap<String, VisitableLocation> locationIdMap,
            final double walkingDistanceMetersPerSecond)
            throws InterruptedException {

        final double maxWalkingDistance
                = walkingDistanceMetersPerSecond * maxDuration.getSeconds();
        final EstimateStorage estimateStorage = new PermanentEstimateStorage(
                serviceDirectory.getMaxCandidateDistanceStore(),
                serviceDirectory.getCandidateDistancesStore(), locationIdMap);
        final PairGenerator pairGenerator = new CompletePairGenerator(
                sectorTable.getSectors(), stopIdMap.values(), centers);

        final DistanceEstimator distanceEstimator = new StoredDistanceEstimator(
                pairGenerator, maxWalkingDistance, endpointDeterminer,
                estimateStorage);
        return distanceEstimator;
    }

    private static BiMap<String, TransitStop> buildTransitStopIdMap(
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

    private static BiMap<String, PointLocation> buildPointIdMap(
            final Set<PointLocation> centerPoints,
            final BiMap<String, TransitStop> transitStops) {
        final ImmutableBiMap.Builder<String, PointLocation> pointMapBuilder
                = ImmutableBiMap.builder();
        pointMapBuilder.putAll(transitStops);

        for (final PointLocation centerPoint : centerPoints) {
            pointMapBuilder.put(centerPoint.getIdentifier(), centerPoint);
        }
        final BiMap<String, PointLocation> pointIdMap
                = pointMapBuilder.build();
        return pointIdMap;
    }

    private static BiMap<String, VisitableLocation> buildLocationIdMap(
            final BiMap<String, PointLocation> pointIdMap,
            final SectorTable sectorTable) {
        final ImmutableBiMap.Builder<String, VisitableLocation> locationMapBuilder
                = ImmutableBiMap.builder();
        locationMapBuilder.putAll(pointIdMap);
        for (final Sector sector : sectorTable.getSectors()) {
            locationMapBuilder.put(sector.getIdentifier(), sector);
        }
        return locationMapBuilder.build();
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
