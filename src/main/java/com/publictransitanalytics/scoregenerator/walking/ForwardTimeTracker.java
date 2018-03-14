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
 * Keeps track of time as it moves forward.
 *
 * @author Public Transit Analytics
 */
public class ForwardTimeTracker implements TimeTracker {

    @Override
    public LocalDateTime adjust(LocalDateTime time, Duration adjustment) {
        return time.plus(adjustment);
    }

    @Override
    public boolean canAdjust(LocalDateTime currentTime, Duration adjustment,
                             LocalDateTime cutoffTime) {
        return !currentTime.plus(adjustment).isAfter(cutoffTime);
    }

    @Override
    public Duration getDuration(LocalDateTime currentTime,
                                LocalDateTime cutoffTime) {
        return Duration.between(currentTime, cutoffTime);
    }

    @Override
    public boolean shouldReplace(final LocalDateTime baseTime,
                                 final LocalDateTime otherTime) {
        return otherTime.isBefore(baseTime);
    }

    @Override
    public boolean shouldReplace(final MovementPath currentPath,
                                 final LocalDateTime otherTime) {
        final ImmutableList<Movement> movements = currentPath.getMovements();
        if (movements.isEmpty()) {
            return false;
        }
        
        final Movement terminal = movements.get(movements.size() - 1);
        return otherTime.isBefore(terminal.getEndTime());
    }

    @Override
    public boolean meetsCutoff(final LocalDateTime time,
                               final LocalDateTime cutoffTime) {
        return !time.isAfter(cutoffTime);
    }

    @Override
    public Iterator<LocalDateTime> getTimeIterator(
            final NavigableSet<LocalDateTime> times) {
        return times.descendingIterator();
    }

}
