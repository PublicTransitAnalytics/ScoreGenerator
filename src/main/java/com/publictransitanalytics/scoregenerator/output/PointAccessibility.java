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
import com.publictransitanalytics.scoregenerator.environment.SectorTable;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * A measurement of Spontaneous Accessibility at a single place.
 *
 * @author Public Transit Analytics
 */
public class PointAccessibility {

    private final AccessibilityType type;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point center;

    private final Map<Bounds, SectorReachInformation> sectorPaths;

    private final String startTime;

    private final String endTime;

    private final String samplingInterval;

    private final String tripDuration;
    
    private final int taskCount;
    
    private final int totalSectors;
    
    private final long inServiceSeconds;

    public PointAccessibility(
            final int taskCount, final PathScoreCard scoreCard,
            final SectorTable sectorTable, final PointLocation centerPoint, 
            final LocalDateTime startTime, final LocalDateTime lastTime, 
            final Duration samplingInterval, final Duration tripDuration,
            final boolean backward, final Duration inServiceTime)
            throws InterruptedException {
        type = AccessibilityType.POINT_ACCESSIBILITY;
        direction = backward ? Direction.INBOUND : Direction.OUTBOUND;
        mapBounds = new Bounds(sectorTable);
        center = new Point(centerPoint);
        this.startTime = startTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.endTime = lastTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);

        this.taskCount = taskCount;
        totalSectors = sectorTable.getSectors().size();

        final ImmutableMap.Builder<Bounds, SectorReachInformation> informationBuilder
                = ImmutableMap.builder();

        for (final Sector sector : sectorTable.getSectors()) {
            final Map<TaskIdentifier, MovementPath> sectorPaths
                    = scoreCard.getBestPaths(sector);
            if (!sectorPaths.isEmpty()) {
                final ImmutableSet.Builder<MovementPath> bestPathsBuilder
                        = ImmutableSet.builder();
                int count = 0;
                for (final MovementPath taskPath : sectorPaths.values()) {
                    if (taskPath != null) {
                        bestPathsBuilder.add(taskPath);
                        count++;
                    }
                }
                final Bounds bounds = new Bounds(sector);
                final Set<LocalDateTime> reachTimes = scoreCard.getReachedTimes(
                        sector);
                final SectorReachInformation information
                        = new SectorReachInformation(
                                bestPathsBuilder.build(), count, reachTimes);
                informationBuilder.put(bounds, information);
            }
        }
        sectorPaths = informationBuilder.build();
        
        inServiceSeconds = inServiceTime.getSeconds();
    }
}
