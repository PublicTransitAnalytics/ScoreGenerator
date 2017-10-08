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
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 *
 * @author Public Transit Analytics
 */
public class ComparativeTimeQualifiedPointAccessibility {

    private final AccessibilityType type;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point center;

    private final Map<Bounds, ComparativeFullSectorReachInformation> sectorPaths;

    private final String time;

    private final String tripDuration;

    private final int totalSectors;

    private final String trialName;

    public ComparativeTimeQualifiedPointAccessibility(
            final PathScoreCard scoreCard, final PathScoreCard trialScoreCard,
            final SectorTable sectorTable, final PointLocation centerPoint,
            final LocalDateTime time, final Duration tripDuration,
            final boolean backward, final String name)
            throws InterruptedException {
        type = AccessibilityType.COMPARATIVE_TIME_QUALIFIED_POINT_ACCESSIBILITY;
        direction = backward ? Direction.INBOUND : Direction.OUTBOUND;
        mapBounds = new Bounds(sectorTable);
        center = new Point(centerPoint);
        this.time = time.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);
        final ImmutableMap.Builder<Bounds, ComparativeFullSectorReachInformation> builder
                = ImmutableMap.builder();

        final TaskIdentifier task = new TaskIdentifier(time, centerPoint);
        for (final Sector sector : sectorTable.getSectors()) {
            final MovementPath sectorPath
                    = scoreCard.getBestPath(sector, task);
            final MovementPath trialSectorPath
                    = trialScoreCard.getBestPath(sector, task);
            if (sectorPath != null || trialSectorPath != null) {
                final Bounds sectorBounds = new Bounds(sector);

                builder.put(sectorBounds,
                            new ComparativeFullSectorReachInformation(
                                    sectorPath, trialSectorPath));
            }
        }
        sectorPaths = builder.build();
        totalSectors = sectorTable.getSectors().size();
        trialName = name;
    }
}
