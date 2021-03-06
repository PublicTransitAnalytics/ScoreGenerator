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
package com.publictransitanalytics.scoregenerator.walking;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.NavigableSet;

/**
 * Keeps track of time as if it's moving backwards, in the context of looking at
 * where a person could have come from.
 *
 * @author Public Transit Analytics
 */
public class BackwardTimeTracker implements TimeTracker {

    @Override
    public LocalDateTime adjust(final LocalDateTime time,
                                final Duration adjustment) {
        return time.minus(adjustment);
    }

    @Override
    public boolean canAdjust(final LocalDateTime currentTime,
                             final Duration adjustment,
                             final LocalDateTime cutoffTime) {
        return !currentTime.minus(adjustment).isBefore(cutoffTime);
    }

    @Override
    public Duration getDuration(final LocalDateTime currentTime,
                                final LocalDateTime cutoffTime) {
        return Duration.between(cutoffTime, currentTime);
    }

    @Override
    public boolean shouldReplace(final LocalDateTime baseTime,
                                 final LocalDateTime otherTime) {
        return otherTime.isAfter(baseTime);
    }
    
    @Override
    public boolean shouldReplace(final MovementPath currentPath,
                                 final LocalDateTime otherTime) {
        final ImmutableList<Movement> movements = currentPath.getMovements();
        if (movements.isEmpty()) {
            return false;
        }
        
        final Movement terminal = movements.get(0);
        return otherTime.isAfter(terminal.getStartTime());
    }

    @Override
    public boolean meetsCutoff(final LocalDateTime time,
                               final LocalDateTime cutoffTime) {
        return !time.isBefore(cutoffTime);
    }

    @Override
    public Iterator<LocalDateTime> getTimeIterator(
            final NavigableSet<LocalDateTime> times) {
        return times.iterator();
    }
}
