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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.opensextant.geodesy.Geodetic2DBounds;

/**
 * A record for drawing a comparative time range probability map.
 *
 * @author Public Transit Analytics
 */
public class ComparativeTimeRangeSectorMap {

    private static final SortedSetMultimap EMPTY_MAP = Multimaps
            .unmodifiableSortedSetMultimap(TreeMultimap.create());

    private final MapType mapType;

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
            final SectorTable baseSectorTable,
            final LocalDate baseDate, final int baseScore,
            final SectorTable trialSectorTable,
            final LocalDate trialDate, final int trialScore,
            final String startingPointString, final LocalTime startTime,
            final Duration span, final Duration samplingInterval,
            final Duration tripDuration, final int buckets) {
        mapType = MapType.COMPARATIVE_TIME_RANGE_SECTOR;
        if (!baseSectorTable.getBounds().equals(trialSectorTable.getBounds())) {
            throw new IllegalArgumentException();
        }

        mapBounds = new Bounds(baseSectorTable);

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

        this.startingPoint = startingPointString;

        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);

        final ImmutableMap.Builder<Bounds, DestinationProbabilityComparison> builder
                = ImmutableMap.builder();
        final ImmutableSet.Builder<Geodetic2DBounds> baseBoundsBuilder
                = ImmutableSet.builder();
        for (final Sector sector : baseSectorTable.getSectors()) {
            final SortedSetMultimap<LocalDateTime, MovementPath> basePaths
                    = sector.getPaths();
            if (!basePaths.isEmpty()) {
                final Geodetic2DBounds bounds = sector.getBounds();
                baseBoundsBuilder.add(bounds);
                final Sector trialSector = trialSectorTable.getSector(bounds);
                final SortedSetMultimap<LocalDateTime, MovementPath> trialPaths
                        = (trialSector == null)
                                ? EMPTY_MAP : trialSector.getPaths();

                builder.put(new Bounds(sector),
                            new DestinationProbabilityComparison(
                                    basePaths, baseDate, trialPaths, trialDate,
                                    startTime, span, samplingInterval,
                                    buckets));
            }

        }
        final Set<Geodetic2DBounds> baseBounds = baseBoundsBuilder.build();

        for (final Sector sector : trialSectorTable.getSectors()) {
            final SortedSetMultimap<LocalDateTime, MovementPath> trialPaths
                    = sector.getPaths();

            if (!trialPaths.isEmpty()) {
                final Geodetic2DBounds bounds = sector.getBounds();
                if (!baseBounds.contains(bounds)) {
                    builder.put(new Bounds(sector),
                                new DestinationProbabilityComparison(
                                        EMPTY_MAP, baseDate, trialPaths,
                                        trialDate, startTime, span,
                                        samplingInterval, buckets));
                }
            }
        }
        sectors = builder.build();
    }

}
