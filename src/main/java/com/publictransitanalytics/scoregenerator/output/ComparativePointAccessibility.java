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
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.environment.StoredGrid;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.scoring.LogicalTask;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author Public Transit Analytics
 */
public class ComparativePointAccessibility {

    private final AccessibilityType type;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point center;

    private final Map<Bounds, ComparativeSectorReachInformation> sectorPaths;

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

    public ComparativePointAccessibility(
            final int taskCount, final int trialTaskCount,
            final PathScoreCard scoreCard, final PathScoreCard trialScoreCard,
            final Grid grid, final PointLocation centerPoint,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final LocalDateTime trialStartTime,
            final LocalDateTime trialEndTime, final Duration samplingInterval,
            final Duration tripDuration, final boolean backward,
            final String trialName, final Duration inServiceTime,
            final Duration trialInServiceTime) throws InterruptedException {
        type = AccessibilityType.COMPARATIVE_POINT_ACCESSIBILITY;
        direction = backward ? Direction.INBOUND : Direction.OUTBOUND;
        mapBounds = new Bounds(grid.getBounds());
        center = new Point(centerPoint);
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

        final Set<Sector> sectors = grid.getAllSectors();
        totalSectors = grid.getReachableSectors().size();

        final ImmutableMap.Builder<Bounds, ComparativeSectorReachInformation> informationBuilder
                = ImmutableMap.builder();

        for (final Sector sector : sectors) {
            final Map<LogicalTask, MovementPath> bestPaths
                    = scoreCard.getBestPaths(sector);
            final Map<LogicalTask, MovementPath> trialBestPaths
                    = trialScoreCard.getBestPaths(sector);
            if (!bestPaths.isEmpty() || !trialBestPaths.isEmpty()) {
                final Set<MovementPath> bestPathSet = getBestPathSet(bestPaths);
                final Set<LocalDateTime> reachTimes = scoreCard.getReachedTimes(
                        sector);
                final Set<MovementPath> trialBestPathSet
                        = getBestPathSet(trialBestPaths);
                final Set<LocalDateTime> trialReachTimes
                        = trialScoreCard.getReachedTimes(sector);
                int numBestPaths = bestPathSet.size();
                int numTrialBestPaths = trialBestPathSet.size();

                final Bounds bounds = new Bounds(sector);
                final ComparativeSectorReachInformation information
                        = new ComparativeSectorReachInformation(
                                bestPathSet, numBestPaths, reachTimes,
                                trialBestPathSet, numTrialBestPaths,
                                trialReachTimes);
                informationBuilder.put(bounds, information);
            }
        }
        sectorPaths = informationBuilder.build();
        this.trialName = trialName;

        inServiceSeconds = inServiceTime.getSeconds();
        trialInServiceSeconds = trialInServiceTime.getSeconds();
    }

    private static Set<MovementPath> getBestPathSet(
            final Map<LogicalTask, MovementPath> taskPaths) {
        final ImmutableSet.Builder<MovementPath> bestPathsBuilder
                = ImmutableSet.builder();

        for (final MovementPath taskPath : taskPaths.values()) {
            if (taskPath != null) {
                bestPathsBuilder.add(taskPath);
            }
        }
        return bestPathsBuilder.build();
    }
}
