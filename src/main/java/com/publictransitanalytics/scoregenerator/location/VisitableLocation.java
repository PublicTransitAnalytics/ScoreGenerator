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
package com.publictransitanalytics.scoregenerator.location;

import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.time.LocalDateTime;
import lombok.Getter;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * Describes a place, whether a point or region, that a person can reach.
 *
 * @author Public Transit Analytics
 */
public abstract class VisitableLocation {

    @Getter
    private final SortedSetMultimap<LocalDateTime, MovementPath> paths;

    public VisitableLocation() {
        paths = Multimaps.synchronizedSortedSetMultimap(TreeMultimap.create());
    }

    public boolean hasNoBetterPath(final LocalDateTime startTime,
                                   final MovementPath path) {
        if (!paths.containsKey(startTime)) {
            return true;
        }
        final MovementPath bestPath = paths.get(startTime).first();

        return path.compareTo(bestPath) < 0;
    }

    public void addPath(final LocalDateTime startTime, final MovementPath path) {
        paths.put(startTime, path);
    }

    public abstract Geodetic2DPoint getNearestPoint(
            final Geodetic2DPoint givenLocation);

    public abstract String getIdentifier();

    public abstract String getCommonName();

    public abstract void accept(Visitor visitor) throws InterruptedException;

    public abstract Geodetic2DPoint getCanonicalPoint();
}
