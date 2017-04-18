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
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;

/**
 * Describes the information pertaining to the rider's arrival at a Sector over
 * a time range.
 *
 * @author Public Transit Analytics
 */
public class DestinationProbabilityInformation {

    final int reachCount;
    final String reachPercentage;
    final int reachBucket;
    final Map<SimplePath, String> pathPercentage;

    DestinationProbabilityInformation(
            final SortedSetMultimap<LocalDateTime, MovementPath> sectorPaths,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final Duration samplingInterval, final int buckets) {
        final int samples
                = (int) (Duration.between(startTime, endTime).getSeconds()
                         / samplingInterval.getSeconds());
        final Multiset<SimplePath> paths = HashMultiset.create();

        LocalDateTime time = startTime;
        int count = 0;
        while (!time.isAfter(endTime)) {
            if (sectorPaths.containsKey(time)) {
                count += 1;
                paths.add(new SimplePath(sectorPaths.get(time).first()));
            }
            time = time.plus(samplingInterval);
        }

        final int bucketSize = samples / buckets;
        reachCount = count;
        reachPercentage = String.format("%.1f%%", ((double) count * 100)
                                                          / samples);
        reachBucket = ((reachCount + bucketSize - 1) * buckets) / samples;

        final TreeMultimap<Integer, SimplePath> frequencyMap
                = TreeMultimap.create(
                        Integer::compareTo,
                        (p1, p2) -> p1.toString().compareTo(p2.toString()));

        for (SimplePath path : paths.elementSet()) {
            frequencyMap.put(paths.count(path), path);
        }

        pathPercentage = new LinkedHashMap<>();
        for (final Integer frequency : frequencyMap.keySet().descendingSet()) {
            final NavigableSet<SimplePath> pathsForFrequency
                    = frequencyMap.get(frequency);
            for (final SimplePath pathForFrequency : pathsForFrequency) {
                final String percentString = String.format(
                        "%.1f%%", ((double) frequency * 100) / samples);
                pathPercentage.put(pathForFrequency, percentString);
            }
        }
    }

}
