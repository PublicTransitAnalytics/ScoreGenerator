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

import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.scoring.PathScoreCard;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * A measurement of Spontaneous Accessibility at a single place and time.
 *
 * @author Public Transit Analytics
 */
public class TimeQualifiedPointAccessibility {

    private final AccessibilityType type;

    private final Direction direction;

    private final Bounds mapBounds;

    private final Point center;

    private final Map<Bounds, FullSectorReachInformation> sectorPaths;

    private final String time;

    private final String tripDuration;

    private final int totalSectors;

    private final long inServiceSeconds;

    public TimeQualifiedPointAccessibility(
            final PathScoreCard scoreCard, final Grid grid,
            final PointLocation centerPoint, final LocalDateTime time,
            final Duration tripDuration, final boolean backward,
            final Duration inServiceTime)
            throws InterruptedException {
        type = AccessibilityType.TIME_QUALIFIED_POINT_ACCESSIBILITY;
        direction = backward ? Direction.INBOUND : Direction.OUTBOUND;
        mapBounds = new Bounds(grid.getBounds());
        center = new Point(centerPoint);
        this.time = time.format(DateTimeFormatter.ofPattern(
                "YYYY-MM-dd HH:mm:ss"));

        this.tripDuration = DurationFormatUtils.formatDurationWords(
                tripDuration.toMillis(), true, true);
        final ImmutableMap.Builder<Bounds, FullSectorReachInformation> builder
                = ImmutableMap.builder();

        final TaskIdentifier task = new TaskIdentifier(time, centerPoint);

        final Set<Sector> sectors = grid.getAllSectors();
        for (final Sector sector : sectors) {
            final MovementPath sectorPath
                    = scoreCard.getBestPath(sector, task);
            if (sectorPath != null) {
                final Bounds sectorBounds = new Bounds(sector);

                builder.put(sectorBounds, new FullSectorReachInformation(
                            sectorPath));
            }
        }
        sectorPaths = builder.build();
        totalSectors = sectors.size();
        inServiceSeconds = inServiceTime.getSeconds();
    }

}
