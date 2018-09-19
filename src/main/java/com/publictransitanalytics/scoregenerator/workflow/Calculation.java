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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.schedule.patching.Transformer;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceDataDirectory;
import com.publictransitanalytics.scoregenerator.distance.DistanceClient;
import com.publictransitanalytics.scoregenerator.distance.DistanceStoreManager;
import com.publictransitanalytics.scoregenerator.distance.EstimatingDistanceClient;
import com.publictransitanalytics.scoregenerator.distance.ForwardPointSequencer;
import com.publictransitanalytics.scoregenerator.distance.OsrmLocalDistanceClient;
import com.publictransitanalytics.scoregenerator.distance.PointSequencerFactory;
import com.publictransitanalytics.scoregenerator.distance.RangedCachingReachabilityClient;
import com.publictransitanalytics.scoregenerator.distance.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.distance.StoreBackedDistanceStoreManager;
import com.publictransitanalytics.scoregenerator.distance.SplitMergeDistanceClient;
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.location.Center;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.LogicalCenter;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.schedule.DirectoryReadingTripCreator;
import com.publictransitanalytics.scoregenerator.schedule.LastTimeScheduleInterpolator;
import com.publictransitanalytics.scoregenerator.schedule.ScheduleInterpolatorFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.LocalDateTime;
import java.util.Set;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.TripCreatingTransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.patching.Patch;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import com.squareup.okhttp.OkHttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
    @Getter
    private final BiMap<String, TransitStop> stopIdMap;
    @Getter
    private final SetMultimap<PointLocation, Sector> pointSectorMap;
    private final Set<GridPoint> gridPoints;
    @Getter
    private final ReachabilityClient reachabilityClient;
    @Getter
    private final RiderFactory riderFactory;

    public Calculation(final Grid grid, final Set<Center> centers,
                       final Duration longestDuration, final boolean backward,
                       final Duration span, final Duration samplingInterval,
                       final double walkingMetersPerSecond,
                       final TimeTracker timeTracker,
                       final Map<String, ServiceDataDirectory> serviceDirectoriesMap,
                       final ScoreCardFactory<S> scoreCardFactory,
                       final LocalDateTime startTime,
                       final ServiceDataDirectory serviceDirectory,
                       final List<Patch> patches, 
                       final Set<TransitStop> addedStops,
                       final Set<TransitStop> deletedStops,
                       final BiMap<String, TransitStop> stopIdMap)
            throws InterruptedException {

        final LocalDateTime endTime = (span != null)
                ? startTime.plus(span) : null;

        this.walkingMetersPerSecond = walkingMetersPerSecond;
        this.longestDuration = longestDuration;
        this.backward = backward;
        this.timeTracker = timeTracker;
        this.stopIdMap = stopIdMap;
        gridPoints = grid.getGridPoints();

        final LocalDateTime earliestTime = getEarliestTime(
                startTime, longestDuration, backward);
        final LocalDateTime latestTime = getLatestTime(
                startTime, endTime, longestDuration, backward);

        final TransitNetwork baseTransitNetwork = buildTransitNetwork(
                serviceDirectory, earliestTime, latestTime, stopIdMap);

        pointSectorMap = buildPointSectorMap(grid, stopIdMap.values());

        final PointSequencerFactory pointSequencerFactory;
        final RiderFactory baseRiderFactory;
        if (!backward) {
            baseRiderFactory = new ForwardRiderFactory(baseTransitNetwork);
            pointSequencerFactory = (origin, destinations)
                    -> new ForwardPointSequencer(origin, destinations);
        } else {
            baseRiderFactory = new RetrospectiveRiderFactory(
                    baseTransitNetwork);
            pointSequencerFactory = (destination, origins)
                    -> new ForwardPointSequencer(destination, origins);
        }

        final DistanceClient distanceClient = buildOsrmDistanceClient(
                pointSequencerFactory, 1000);
        final Set<PointLocation> centerPoints = centers.stream()
                .map(Center::getPhysicalCenters).flatMap(Collection::stream)
                .collect(Collectors.toSet());

        final BiMap<String, PointLocation> basePointIdMap = buildPointIdMap(
                centerPoints, stopIdMap, grid);

        taskGroups = getTaskGroups(centers);
        times = getTaskTimes(startTime, endTime, samplingInterval);

        final DistanceClient estimator = new EstimatingDistanceClient(
                walkingMetersPerSecond);
        final DistanceStoreManager storeManager
                = new StoreBackedDistanceStoreManager(
                        serviceDirectory.getWalkingTimeStore(),
                        serviceDirectory.getMaxWalkingTimeStore(),
                        basePointIdMap);
        final ReachabilityClient baseReachabilityClient
                = new RangedCachingReachabilityClient(
                        storeManager, basePointIdMap.values(),
                        timeTracker, distanceClient, estimator);

        allowedModes = ImmutableSet.of(ModeType.TRANSIT, ModeType.WALKING);


        final Transformer transformer = new Transformer(
                timeTracker, baseTransitNetwork, backward, longestDuration,
                walkingMetersPerSecond, distanceClient, estimator,
                basePointIdMap, baseReachabilityClient, storeManager,
                baseRiderFactory);
        
        for (final Patch patch : patches) {
            transformer.addTripPatch(patch);
        }
        for (final TransitStop stop : addedStops) {
            transformer.addStop(stop);
        }
        for (final TransitStop stop : deletedStops) {
            transformer.deleteStop(stop);
        }

        final Set<LogicalCenter> logicalCenters = centers.stream()
                .map(Center::getLogicalCenter).collect(Collectors.toSet());
        scoreCard = scoreCardFactory.makeScoreCard(
                times.size() * logicalCenters.size(), pointSectorMap);

        transitNetwork = transformer.getTransitNetwork();
        riderFactory = transformer.getRiderFactory();
        reachabilityClient = transformer.getReachabilityClient();
    }

    private static DistanceClient buildOsrmDistanceClient(
            final PointSequencerFactory pointSequencerFactory,
            final int maxConsidered) {
        final DistanceClient osrmDistanceClient = new OsrmLocalDistanceClient(
                new OkHttpClient(), "localhost", 5000, pointSequencerFactory);

        return new SplitMergeDistanceClient(osrmDistanceClient, maxConsidered);
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

    private static BiMap<String, PointLocation> buildPointIdMap(
            final Set<PointLocation> centerPoints,
            final BiMap<String, TransitStop> transitStops, final Grid grid) {
        final ImmutableBiMap.Builder<String, PointLocation> pointMapBuilder
                = ImmutableBiMap.builder();
        pointMapBuilder.putAll(transitStops);

        for (final PointLocation centerPoint : centerPoints) {
            pointMapBuilder.put(centerPoint.getIdentifier(), centerPoint);
        }

        final Set<? extends PointLocation> gridPoints = Sets.difference(
                grid.getGridPoints(), centerPoints);
        for (final PointLocation gridPoint : gridPoints) {
            pointMapBuilder.put(gridPoint.getIdentifier(), gridPoint);
        }
        final BiMap<String, PointLocation> pointIdMap
                = pointMapBuilder.build();
        return pointIdMap;
    }

    private static Set<TaskGroupIdentifier> getTaskGroups(
            final Set<Center> centerPoints) {
        return centerPoints.stream().map(TaskGroupIdentifier::new)
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
            throws InterruptedException {

        final ScheduleInterpolatorFactory interpolatorFactory = (baseTime)
                -> new LastTimeScheduleInterpolator(baseTime);

        final TransitNetwork transitNetwork = new TripCreatingTransitNetwork(
                new DirectoryReadingTripCreator(
                        earliestTime, latestTime,
                        serviceData.getStopTimesDirectory(),
                        serviceData.getRouteDetailsDirectory(),
                        serviceData.getTripDetailsDirectory(),
                        serviceData.getServiceTypeCalendar(),
                        stopIdMap, interpolatorFactory));
        return transitNetwork;
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
                if (sectors.isEmpty()) {
                    log.warn("{} ({}) is not within the bounds.",
                             stop.getCommonName(), 
                             stop.getLocation().toDegreeString());
                } else {
                    builder.putAll(stop, sectors);
                }
            }
        }
        return builder.build();
    }

}
