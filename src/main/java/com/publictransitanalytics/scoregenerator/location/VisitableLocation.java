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
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * Describes a place, whether a point or region, that a person can reach.
 *
 * @author Public Transit Analytics
 */
public abstract class VisitableLocation {

    @Getter
    private final Map<TaskIdentifier, MovementPath> bestPaths;

    public VisitableLocation() {
        bestPaths = new ConcurrentHashMap<>();
    }

    public boolean hasNoBetterPath(final TaskIdentifier task,
                                   final MovementPath path) {
        if (!bestPaths.containsKey(task)) {
            return true;
        }
        final MovementPath bestPath = bestPaths.get(task);


        return path.compareTo(bestPath) < 0;
    }

    public void replacePath(final TaskIdentifier task, final MovementPath path) {
        bestPaths.put(task, path);
    }

    public abstract Geodetic2DPoint getNearestPoint(
            final Geodetic2DPoint givenLocation);

    public abstract String getIdentifier();

    public abstract String getCommonName();

    public abstract void accept(Visitor visitor) throws InterruptedException;

    public abstract Geodetic2DPoint getCanonicalPoint();
}
