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
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Network Accessibility measurement.
 *
 * @author Public Transit Analytics
 */
public class NetworkAccessibility {

    private final AccessibilityType type;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point boundsCenter;

    private final Map<Bounds, Integer> sectorCounts;

    private final Set<Point> centerPoints;

    private final int sampleCount;

    private final String startTime;

    private final String endTime;

    private final String samplingInterval;

    private final String tripDuration;

    private final int taskCount;

    private final int totalSectors;

    private final long inServiceSeconds;

    public NetworkAccessibility(
            final int taskCount, final ScoreCard scoreCard,
            final Grid grid, final Set<Sector> centerSectors,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration tripDuration, final Duration samplingInterval,
            final boolean backward, final Duration inServiceTime)
            throws InterruptedException {
        type = AccessibilityType.NETWORK_ACCESSIBILITY;

        direction = backward ? Direction.INBOUND : Direction.OUTBOUND;
        mapBounds = new Bounds(grid.getBounds());
        boundsCenter = new Point(grid.getBounds().getCenter());
        this.startTime = startTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.endTime = endTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);

        this.taskCount = taskCount;
        
        final Set<Sector> sectors = grid.getAllSectors();
        totalSectors = grid.getReachableSectors().size();

        final ImmutableMap.Builder<Bounds, Integer> countBuilder
                = ImmutableMap.builder();
        for (final Sector sector : sectors) {
            if (scoreCard.hasPath(sector)) {
                final Bounds bounds = new Bounds(sector);
                countBuilder.put(bounds,
                                 scoreCard.getReachedCount(sector));
            }
        }
        sectorCounts = countBuilder.build();

        this.centerPoints = centerSectors.stream().map(
                sector -> new Point(sector.getBounds().getCenter()))
                .collect(Collectors.toSet());
        sampleCount = centerPoints.size();

        inServiceSeconds = inServiceTime.getSeconds();
    }
}
