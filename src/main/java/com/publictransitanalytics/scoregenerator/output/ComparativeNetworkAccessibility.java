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
import com.google.common.collect.Sets;
import com.publictransitanalytics.scoregenerator.environment.SectorTable;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author Public Transit Analytics
 */
public class ComparativeNetworkAccessibility {

    private final AccessibilityType type;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point boundsCenter;

    private final Map<Bounds, CountComparison> sectorCounts;

    private final Set<Point> centerPoints;

    private final int sampleCount;

    private final String startTime;

    private final String endTime;

    private final String trialStartTime;

    private final String trialEndTime;

    private final String samplingInterval;

    private final String tripDuration;

    private final int taskCount;

    private final int trialTaskCount;

    private final int totalSectors;

    private final String trialName;

    private final long inServiceSeconds;

    private final long trialInServiceSeconds;

    public ComparativeNetworkAccessibility(
            final int taskCount, final int trialTaskCount,
            final ScoreCard scoreCard, final ScoreCard trialScoreCard,
            final SectorTable sectorTable,
            final Set<PointLocation> centerPoints,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final LocalDateTime trialStartTime,
            final LocalDateTime trialEndTime, final Duration tripDuration,
            final Duration samplingInterval, final boolean backward,
            final String trialName, final Duration inServiceTime,
            final Duration trialInServiceTime) throws InterruptedException {
        type = AccessibilityType.COMPARATIVE_NETWORK_ACCESSIBILITY;

        direction = backward ? Direction.INBOUND : Direction.OUTBOUND;
        mapBounds = new Bounds(sectorTable);
        boundsCenter = new Point(sectorTable.getBounds().getCenter());
        this.startTime = startTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.endTime = endTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.trialStartTime = trialStartTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.trialEndTime = trialEndTime.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));
        this.samplingInterval = DurationFormatUtils.formatDurationWords(
                samplingInterval.toMillis(), true, true);

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);

        this.taskCount = taskCount;
        this.trialTaskCount = trialTaskCount;
        totalSectors = sectorTable.getSectors().size();

        final ImmutableMap.Builder<Bounds, CountComparison> countBuilder
                = ImmutableMap.builder();
        for (final Sector sector : sectorTable.getSectors()) {
            final int count = scoreCard.hasPath(sector)
                    ? scoreCard.getReachedCount(sector) : 0;
            final int trialCount = trialScoreCard.hasPath(sector)
                    ? trialScoreCard.getReachedCount(sector) : 0;

            if (count != 0 && trialCount != 0) {
                final Bounds bounds = new Bounds(sector);
                final CountComparison comparison
                        = new CountComparison(count, trialCount);
                countBuilder.put(bounds, comparison);
            }
        }

        sectorCounts = countBuilder.build();

        this.centerPoints = centerPoints.stream().map(
                point -> new Point(point)).collect(Collectors.toSet());
        sampleCount = centerPoints.size();

        this.trialName = trialName;

        inServiceSeconds = inServiceTime.getSeconds();
        trialInServiceSeconds = trialInServiceTime.getSeconds();
    }
}
