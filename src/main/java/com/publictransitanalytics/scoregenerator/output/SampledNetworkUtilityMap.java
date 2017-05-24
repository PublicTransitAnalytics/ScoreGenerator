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
package com.publictransitanalytics.scoregenerator.output;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author Public Transit Analytics
 */
public class SampledNetworkUtilityMap {

    private final MapType mapType;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Map<Bounds, DestinationProbabilityInformation> sectors;

    private final List<MutualInformationMeasurement> centerPointMutualInformation;

    private final Point firstPoint;

    private final String startTime;

    private final String endTime;

    private final String samplingInterval;

    private final String tripDuration;

    private final int score;

    public SampledNetworkUtilityMap(final Set<TaskIdentifier> tasks,
                                    final SectorTable sectorTable,
                                    final PointLocation firstPoint,
                                    final LinkedHashMap<PointLocation, Double> centerPointMutualInformation,
                                    final LocalDateTime startTime,
                                    final LocalDateTime endTime,
                                    final Duration tripDuration,
                                    final Duration samplingInterval,
                                    final boolean backward,
                                    final int buckets, final int score) {
        mapType = MapType.CUMULATIVE_POINT_UTILITY;

        direction = backward ? Direction.TO_POINT : Direction.FROM_POINT;
        mapBounds = new Bounds(sectorTable);
        this.startTime = startTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.endTime = endTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);
        this.score = score;

        final int samples = tasks.size();
        final Multiset<Sector> sectorReachabilities = HashMultiset.create();
        for (final Sector sector : sectorTable.getSectors()) {
            for (final TaskIdentifier task : tasks) {

                final Map<TaskIdentifier, MovementPath> sectorPaths
                        = sector.getBestPaths();
                final MovementPath taskPath = sectorPaths.get(task);
                if (taskPath != null) {
                    sectorReachabilities.add(sector);
                }
            }
        }

        final ImmutableMap.Builder<Bounds, DestinationProbabilityInformation> informationBuilder
                = ImmutableMap.builder();
        for (final Sector sector : sectorReachabilities.elementSet()) {
            final Bounds bounds = new Bounds(sector);
            final DestinationProbabilityInformation information
                    = new DestinationProbabilityInformation(
                            null, samples, sectorReachabilities.count(sector),
                            buckets);
            informationBuilder.put(bounds, information);
        }
        sectors = informationBuilder.build();

        final ImmutableList.Builder<MutualInformationMeasurement> builder
                = ImmutableList.builder();
        for (final Map.Entry<PointLocation, Double> entry
             : centerPointMutualInformation.entrySet()) {
            builder.add(new MutualInformationMeasurement(
                    new Point(entry.getKey()), entry.getValue()));
        }
        this.centerPointMutualInformation = builder.build();
        this.firstPoint = new Point(firstPoint);
    }
}
