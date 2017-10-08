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

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
class ComparativeSectorReachInformation {
    
    private final int reachCount;
    private final Map<SimplePath, Integer> pathCounts;
    private final int trialReachCount;
    private final Map<SimplePath, Integer> trialPathCounts;
    
    public ComparativeSectorReachInformation(
            final Set<MovementPath> bestPaths, final int count,
            final Set<MovementPath> trialBestPaths, final int trialCount) 
            throws InterruptedException {

        reachCount = count;
        trialReachCount = trialCount;
        pathCounts = getPathCounts(bestPaths);
        trialPathCounts = getPathCounts(trialBestPaths);
    }
    
    private static Map<SimplePath, Integer> getPathCounts(
            final Set<MovementPath> bestPaths) throws InterruptedException {
        final Map<SimplePath, Integer> pathCounts;
        final TreeMultimap<Integer, SimplePath> frequencyMap
                = TreeMultimap.create(
                        Integer::compareTo,
                        (p1, p2) -> p1.toString().compareTo(p2.toString()));

        if (bestPaths != null) {
            final ImmutableMultiset.Builder<SimplePath> bestSimplePathsBuilder
                    = ImmutableMultiset.builder();
            for (final MovementPath bestPath : bestPaths) {
                bestSimplePathsBuilder.add(new SimplePath(bestPath));
            }
            final ImmutableMultiset<SimplePath> bestSimplePaths
                    = bestSimplePathsBuilder.build();

            for (final SimplePath path : bestSimplePaths.elementSet()) {
                frequencyMap.put(bestSimplePaths.count(path), path);
            }

            pathCounts = new LinkedHashMap<>();
            for (final Integer frequency
                         : frequencyMap.keySet().descendingSet()) {
                final NavigableSet<SimplePath> pathsForFrequency
                        = frequencyMap.get(frequency);
                for (final SimplePath pathForFrequency : pathsForFrequency) {
                    pathCounts.put(pathForFrequency, frequency);
                }
            }
        } else {
            pathCounts = null;
        }
        return pathCounts;
    }
    
}
