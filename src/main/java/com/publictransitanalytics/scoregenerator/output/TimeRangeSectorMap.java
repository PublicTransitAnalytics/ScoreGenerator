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
import com.google.common.collect.SortedSetMultimap;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * A record for drawing a time range probability map.
 *
 * @author Public Transit Analytics
 */
public class TimeRangeSectorMap {

    private final MapType mapType;
    
    private final Direction direction;

    private final Bounds mapBounds;

    private final Point startingPoint;

    private final Map<Bounds, DestinationProbabilityInformation> sectors;

    private final String startTime;

    private final String endTime;

    private final String samplingInterval;

    private final String tripDuration;

    private final int score;

    public TimeRangeSectorMap(
            final SectorTable sectorTable, final PointLocation startPoint,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration samplingInterval, final Duration tripDuration,
            final boolean backward, final int score, final int buckets) {
        mapType = MapType.TIME_RANGE_SECTOR;
        direction = backward ? Direction.TO_POINT : Direction.FROM_POINT;
        mapBounds = new Bounds(sectorTable);
        startingPoint = new Point(startPoint);
        this.startTime = startTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.endTime = endTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);
        this.score = score;
        final ImmutableMap.Builder<Bounds, DestinationProbabilityInformation> builder
                = ImmutableMap.builder();
        for (final Sector sector : sectorTable.getSectors()) {
            final SortedSetMultimap<LocalDateTime, MovementPath> sectorPaths
                    = sector.getPaths();
            if (!sectorPaths.isEmpty()) {
                final Bounds sectorBounds = new Bounds(sector);

                builder.put(
                        sectorBounds,
                        new DestinationProbabilityInformation(
                                sectorPaths, startTime, endTime,
                                samplingInterval, buckets));
            }
        }
        sectors = builder.build();
    }

}