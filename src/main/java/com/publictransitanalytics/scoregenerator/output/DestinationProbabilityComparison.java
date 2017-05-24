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
import com.google.common.collect.Multisets;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;

/**
 *
 * @author Public Transit Analytics
 */
public class DestinationProbabilityComparison {

    private final int reachBucket;

    private final int baseReachCount;
    private final String baseReachPercentage;
    private final SimplePath baseModePath;

    private final int trialReachCount;
    private final String trialReachPercentage;
    private final SimplePath trialModePath;

    public DestinationProbabilityComparison(
            final int baseCount, final Multiset<MovementPath> baseBestPaths,
            final int trialCount, final Multiset<MovementPath> trialBestPaths,
            final int samples, final int buckets) {

        final int bucketSize = samples / buckets;

        
        if (baseCount > 0) {
            baseModePath = new SimplePath(Multisets.copyHighestCountFirst(
                    baseBestPaths).iterator().next());
        } else {
            baseModePath = null;
        }
        baseReachPercentage = getReachPercentage(baseCount, samples);
        int baseBucket = ((baseCount + bucketSize - 1) * buckets)
                                 / samples;
        baseReachCount = baseCount;

        if (trialCount > 0) {
            trialModePath = new SimplePath(Multisets.copyHighestCountFirst(
                    trialBestPaths).iterator().next());
        } else {
            trialModePath = null;
        }
        trialReachPercentage = getReachPercentage(trialCount, samples);
        int trialBucket = ((trialCount + bucketSize - 1) * buckets)
                                  / samples;
        trialReachCount = trialCount;

        reachBucket = trialBucket - baseBucket;
    }

    private static String getReachPercentage(
            final int count, final int samples) {
        return String.format("%.1f%%", ((double) count * 100) / samples);
    }

}
