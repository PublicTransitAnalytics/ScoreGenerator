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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedSetMultimap;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Describes the information pertaining to the rider's arrival at a Sector over
 * a time range.
 *
 * @author Public Transit Analytics
 */
public class DestinationProbabilityInformation {

    final int reachCount;
    final int reachBucket;
    final Map<SimplePath, Integer> pathCounts;

    DestinationProbabilityInformation(
            final SortedSetMultimap<LocalDateTime, MovementPath> sectorPaths,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration samplingInterval, final int buckets) {
        final LocalDateTime time = startTime;
        final int samples
                = (int) (Duration.between(startTime, endTime).getSeconds()
                         / samplingInterval.getSeconds());
        final Multiset<SimplePath> paths = HashMultiset.create();

        int count = 0;
        while (!time.isAfter(endTime)) {
            time.plus(samplingInterval);
            count += sectorPaths.containsKey(time) ? 1 : 0;
            paths.add(new SimplePath(sectorPaths.get(time).first()));
        }

        reachCount = count;
        reachBucket = (reachCount * buckets) / samples;
        pathCounts = paths.stream().collect(Collectors.toMap(
                k -> k, k -> paths.count(k)));
    }

}
