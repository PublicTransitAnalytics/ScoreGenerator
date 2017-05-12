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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedSet;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * A record for drawing a comparative time range probability map.
 *
 * @author Public Transit Analytics
 */
public class ComparativeTimeRangeSectorMap {

    private final MapType mapType;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Map<Bounds, DestinationProbabilityComparison> sectors;

    private final int baseScore;
    private final String baseStartTime;
    private final String baseEndTime;

    private final int trialScore;
    private final String trialStartTime;
    private final String trialEndTime;

    private final String startingPoint;

    private final String samplingInterval;

    private final String tripDuration;

    public ComparativeTimeRangeSectorMap(
            final SectorTable sectorTable,
            final LocalDate baseDate, final int baseScore,
            final String baseExperimentName,
            final LocalDate trialDate, final int trialScore,
            final String trialExperimentName,
            final PointLocation centerPoint, final LocalTime startTime,
            final Duration span, final Duration samplingInterval,
            final Duration tripDuration, final boolean backward,
            final int buckets) {
        mapType = MapType.COMPARATIVE_TIME_RANGE_SECTOR;

        direction = backward ? Direction.TO_POINT : Direction.FROM_POINT;

        mapBounds = new Bounds(sectorTable);

        this.baseScore = baseScore;
        this.baseStartTime = baseDate.atTime(startTime)
                .format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));
        this.baseEndTime = baseDate.atTime(startTime).plus(span)
                .format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));

        this.trialScore = trialScore;
        this.trialStartTime = trialDate.atTime(startTime)
                .format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));
        this.trialEndTime = trialDate.atTime(startTime).plus(span)
                .format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));

        this.startingPoint = centerPoint.getCommonName();

        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);

        final int samples = getSamples(span, samplingInterval);

        final ImmutableMap.Builder<Bounds, DestinationProbabilityComparison> comparisonsBuilder
                = ImmutableMap.builder();
        for (final Sector sector : sectorTable.getSectors()) {
            final ImmutableMultiset.Builder<MovementPath> baseBestPathsBuilder
                    = ImmutableMultiset.builder();
            final ImmutableMultiset.Builder<MovementPath> trialBestPathsBuilder
                    = ImmutableMultiset.builder();
            final Map<TaskIdentifier, MovementPath> sectorPaths
                    = sector.getBestPaths();

            if (!sectorPaths.isEmpty()) {
                LocalTime time = startTime;
                int baseCount = 0;
                int trialCount = 0;
                while (time.isBefore(startTime.plus(span))) {
                    final LocalDateTime baseTime = baseDate.atTime(time);
                    final TaskIdentifier baseTask = new TaskIdentifier(
                            baseTime, centerPoint, baseExperimentName);
                    final MovementPath basePath
                            = sectorPaths.get(baseTask);
                    if (basePath != null) {
                        baseCount++;
                        baseBestPathsBuilder.add(basePath);
                    }

                    final LocalDateTime trialTime = trialDate.atTime(time);
                    final TaskIdentifier trialTask = new TaskIdentifier(
                            trialTime, centerPoint, baseExperimentName);
                    final MovementPath trialPaths
                            = sectorPaths.get(trialTask);
                    if (trialPaths != null) {
                        trialCount++;
                        trialBestPathsBuilder.add(trialPaths);
                    }
                }

                final Bounds bounds = new Bounds(sector);
                final DestinationProbabilityComparison comparison
                        = new DestinationProbabilityComparison(
                                baseCount, baseBestPathsBuilder.build(),
                                trialCount, trialBestPathsBuilder.build(),
                                samples, buckets);
                        
                
                comparisonsBuilder.put(bounds, comparison);
                       
            }

        }
        sectors = comparisonsBuilder.build();
    }

    private static int getSamples(final Duration span,
                                  final Duration samplingInterval) {
        return (int) (span.getSeconds() / samplingInterval.getSeconds());
    }

}
