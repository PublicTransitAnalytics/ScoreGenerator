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

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
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
            final Multiset<MovementPath> bestPaths, final int samples,
            final int count, final int buckets) {

        final int bucketSize = samples / buckets;
        reachCount = count;
        reachPercentage = String.format(
                "%.1f%%", ((double) count * 100) / samples);
        reachBucket = ((reachCount + bucketSize - 1) * buckets) / samples;

        final TreeMultimap<Integer, SimplePath> frequencyMap
                = TreeMultimap.create(
                        Integer::compareTo,
                        (p1, p2) -> p1.toString().compareTo(p2.toString()));

        if (bestPaths != null) {
            for (MovementPath path : bestPaths.elementSet()) {
                frequencyMap.put(bestPaths.count(path), new SimplePath(path));
            }

            pathPercentage = new LinkedHashMap<>();
            for (final Integer frequency
                         : frequencyMap.keySet().descendingSet()) {
                final NavigableSet<SimplePath> pathsForFrequency
                        = frequencyMap.get(frequency);
                for (final SimplePath pathForFrequency : pathsForFrequency) {
                    final String percentString = String.format(
                            "%.1f%%", ((double) frequency * 100) / samples);
                    pathPercentage.put(pathForFrequency, percentString);
                }
            }
        } else {
            pathPercentage = null;
        }
       
    }

}
