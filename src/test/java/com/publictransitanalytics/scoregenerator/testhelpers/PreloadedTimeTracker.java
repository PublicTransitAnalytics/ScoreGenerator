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
package com.publictransitanalytics.scoregenerator.testhelpers;

import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedTimeTracker implements TimeTracker {

    private final LocalDateTime adjustedTime;
    private final boolean canAdjust;
    private final Duration duration;

    @Override
    public LocalDateTime adjust(final LocalDateTime time,
                                final Duration adjustment) {
        return adjustedTime;
    }

    @Override
    public boolean canAdjust(final LocalDateTime currentTime,
                             final Duration adjustment,
                             final LocalDateTime cutoffTime) {
        return canAdjust;
    }

    @Override
    public Duration getDuration(final LocalDateTime currentTime,
                                final LocalDateTime cutoffTime) {
        return duration;
    }

    @Override
    public boolean shouldReplace(final LocalDateTime baseTime,
                                 final LocalDateTime otherTime) {
        return true;
    }

    @Override
    public boolean meetsCutoff(final LocalDateTime time,
                               final LocalDateTime cutoffTime) {
        return true;
    }

    @Override
    public Iterator<LocalDateTime> getTimeIterator(
            final NavigableSet<LocalDateTime> times) {
        return Collections.emptyIterator();
    }

    @Override
    public boolean shouldReplace(MovementPath currentPath,
                                 LocalDateTime otherTime) {
        return true;
    }

}
