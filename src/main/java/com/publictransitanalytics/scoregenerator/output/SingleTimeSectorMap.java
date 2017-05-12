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

import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedSet;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * A record for drawing a single-time sector map.
 *
 * @author Public Transit Analytics
 */
public class SingleTimeSectorMap {

    private final MapType mapType;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point startingPoint;

    private final Map<Bounds, DestinationInformation> sectors;

    private final String time;

    private final String tripDuration;

    private final int score;

    public SingleTimeSectorMap(
            final SectorTable sectorTable, final PointLocation startPoint,
            final LocalDateTime startTime, final String experimentName,
            final Duration tripDuration, final boolean backward,
            final int score) {
        mapType = MapType.SINGLE_TIME_SECTOR;
        direction = backward ? Direction.TO_POINT : Direction.FROM_POINT;
        mapBounds = new Bounds(sectorTable);
        startingPoint = new Point(startPoint);
        this.time = startTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);
        this.score = score;
        final ImmutableMap.Builder<Bounds, DestinationInformation> builder
                = ImmutableMap.builder();

        final TaskIdentifier task = new TaskIdentifier(startTime, startPoint,
                                                       experimentName);

        for (final Sector sector : sectorTable.getSectors()) {
            final MovementPath sectorPath
                    = sector.getBestPaths().get(task);
            if (sectorPath != null) {
                final Bounds sectorBounds = new Bounds(sector);

                builder.put(sectorBounds, 
                            new DestinationInformation(sectorPath));
            }
        }
        sectors = builder.build();
    }

}
