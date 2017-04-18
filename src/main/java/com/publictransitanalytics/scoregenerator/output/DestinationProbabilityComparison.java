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
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.SortedSetMultimap;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
            final SortedSetMultimap<LocalDateTime, MovementPath> basePaths,
            final LocalDate baseDate,
            final SortedSetMultimap<LocalDateTime, MovementPath> trialPaths,
            final LocalDate trialDate, final LocalTime startTime,
            final Duration span, final Duration samplingInterval,
            final int buckets) {

        final int samples = getSamples(span, samplingInterval);
        final int bucketSize = samples / buckets;

        final Multiset<SimplePath> basePathsMultiset = getPathsMultiset(
                startTime, span, baseDate, basePaths, samplingInterval);

        baseReachCount = basePathsMultiset.size();
        if (baseReachCount > 0) {
            baseModePath = Multisets.copyHighestCountFirst(basePathsMultiset)
                    .iterator().next();
        } else {
            baseModePath = null;
        }
        baseReachPercentage = getReachPercentage(baseReachCount, samples);
        int baseBucket = ((baseReachCount + bucketSize - 1) * buckets)
                                 / samples;

        final Multiset<SimplePath> trialPathsMultiset = getPathsMultiset(
                startTime, span, trialDate, trialPaths, samplingInterval);

        trialReachCount = trialPathsMultiset.size();
        if (trialReachCount > 0) {
            trialModePath = Multisets.copyHighestCountFirst(
                    trialPathsMultiset).iterator().next();
        } else {
            trialModePath = null;
        }
        trialReachPercentage = getReachPercentage(trialReachCount, samples);
        int trialBucket = ((trialReachCount + bucketSize - 1) * buckets)
                                  / samples;

        reachBucket = trialBucket - baseBucket;
    }

    private static String getReachPercentage(
            final int count, final int samples) {
        return String.format("%.1f%%", ((double) count * 100) / samples);
    }

    private static int getSamples(final Duration span,
                                  final Duration samplingInterval) {
        return (int) (span.getSeconds() / samplingInterval.getSeconds());
    }

    private static Multiset<SimplePath> getPathsMultiset(
            final LocalTime startTime, final Duration span,
            final LocalDate date,
            final SortedSetMultimap<LocalDateTime, MovementPath> sectorPaths,
            final Duration samplingInterval) {
        final ImmutableMultiset.Builder<SimplePath> builder
                = ImmutableMultiset.builder();
        LocalDateTime time = date.atTime(startTime);
        final LocalDateTime endDateTime = time.plus(span);
        int count = 0;
        while (!time.isAfter(endDateTime)) {
            if (sectorPaths.containsKey(time)) {
                builder.add(new SimplePath(sectorPaths.get(time).first()));
            }
            time = time.plus(samplingInterval);
        }
        return builder.build();
    }

}
