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
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * The output for a single point that maps the reachable points.
 * 
 * @author Public Transit Analytics
 */
public class SingleTimePointMap {

    private final MapType mapType;

    private final Bounds mapBounds;

    private final Point startingPoint;

    private final Map<Point, SimplePath> paths;

    private final LocalDateTime time;

    private final Duration tripDuration;

    private final float score;

    public SingleTimePointMap(
            final SectorTable sectorTable, 
            final Set<PointLocation> pointLocations,
            final PointLocation startPoint, final LocalDateTime time, 
            final Duration tripDuration) {
        mapType = MapType.SINGLE_TIME_SECTOR;
        mapBounds = new Bounds(sectorTable);
        startingPoint = new Point(startPoint);
        this.time = time;
        this.tripDuration = tripDuration;
        score = 0;
        final ImmutableMap.Builder<Point, SimplePath> mapBuilder
                = ImmutableMap.builder();
        for (final PointLocation pointLocation : pointLocations) {
            final SortedSet<MovementPath> pointPaths = 
                    pointLocation.getPaths().get(time);
            if (!pointPaths.isEmpty()) {
                final Point point = new Point(pointLocation);
                final SimplePath path = new SimplePath(pointPaths.first());
                mapBuilder.put(point, path);
            }
        }
        paths = mapBuilder.build();
    }

}
