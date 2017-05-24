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
package com.publictransitanalytics.scoregenerator.probability;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Map;

/**
 *
 * @author Public Transit Analytics
 */
public class JointProbabilityDistribution<T> {

    final Table<Integer, Integer, Double> distribution;

    public JointProbabilityDistribution(
            final Map<T, Integer> xValues, final Map<T, Integer> yValues,
            final int numBuckets) {

        final int setSize = xValues.size();
        final Table<Integer, Integer, Integer> counts = HashBasedTable.create();
        for (final T randomVariable : xValues.keySet()) {
            final int xValue = xValues.get(randomVariable);
            final int yValue = yValues.get(randomVariable);
            final Integer count = counts.get(xValue, yValue);
            final int replacement;
            if (count == null) {
                replacement = 1;
            } else {
                replacement = count + 1;
            }
            counts.put(xValue, yValue, replacement);
        }

        final ImmutableTable.Builder<Integer, Integer, Double> distributionBuilder
                = ImmutableTable.builder();
        for (int x = 0; x < numBuckets; x++) {
            for (int y = 0; y < numBuckets; y++) {
                if (counts.contains(x, y)) {
                    distributionBuilder.put(x, y, ((double) counts.get(x, y))
                                                          / setSize);
                } else {
                    distributionBuilder.put(x, y, 0.0);
                }

            }
        }
        distribution = distributionBuilder.build();
    }

    public double getProbability(final int x, final int y) {
        return distribution.get(x, y);
    }

    public double getXMarginal(final int x) {
        return distribution.row(x).values().stream().mapToDouble(i -> i).sum();
    }

    public double getYMarginal(final int y) {
        return distribution.column(y).values().stream().mapToDouble(i -> i)
                .sum();
    }

    public double getMutualInformation() {
        double sum = 0;
        for (int x = 0; x < distribution.rowKeySet().size(); x++) {
            for (int y = 0; y < distribution.columnKeySet().size(); y++) {
                final double probability = getProbability(x, y);
                if (probability != 0) {
                    sum += probability * Math.log(
                            probability / (getXMarginal(x) * getYMarginal(y)))
                                   / Math.log(2);
                }

            }
        }
        return sum;
    }

}
