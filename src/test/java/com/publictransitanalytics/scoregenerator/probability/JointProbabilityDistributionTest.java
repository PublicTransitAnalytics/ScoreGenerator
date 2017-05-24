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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class JointProbabilityDistributionTest {

    private static final double DELTA = 0.0001;

    @Test
    public void testJointProbabilityForNonexistent() {
        final Map<String, Integer> xValues = ImmutableMap.of("A", 1, "B", 2);
        final Map<String, Integer> yValues = ImmutableMap.of("A", 1, "B", 0);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(0.0, distribution.getProbability(0, 0), DELTA);
    }

    @Test
    public void testJointProbabilityForExistent() {
        final Map<String, Integer> xValues
                = ImmutableMap.of("A", 1, "B", 2, "C", 1);
        final Map<String, Integer> yValues
                = ImmutableMap.of("A", 1, "B", 0, "C", 1);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(2.0 / 3.0, distribution.getProbability(1, 1), DELTA);
    }

    @Test
    public void testXMarginal() {
        final Map<String, Integer> xValues = ImmutableMap.of("A", 1, "B", 2);
        final Map<String, Integer> yValues = ImmutableMap.of("A", 1, "B", 0);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(0.5, distribution.getXMarginal(2), DELTA);
    }

    @Test
    public void testXMarginalForNonexistent() {
        final Map<String, Integer> xValues = ImmutableMap.of("A", 1, "B", 2);
        final Map<String, Integer> yValues = ImmutableMap.of("A", 1, "B", 0);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(0.0, distribution.getXMarginal(0), DELTA);
    }

    @Test
    public void testYMarginal() {
        final Map<String, Integer> xValues
                = ImmutableMap.of("A", 1, "B", 2, "C", 0);
        final Map<String, Integer> yValues
                = ImmutableMap.of("A", 1, "B", 0, "C", 1);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(2.0 / 3.0, distribution.getYMarginal(1), DELTA);
    }

    @Test
    public void testYMarginalForNonexistent() {
        final Map<String, Integer> xValues
                = ImmutableMap.of("A", 1, "B", 2, "C", 0);
        final Map<String, Integer> yValues
                = ImmutableMap.of("A", 1, "B", 0, "C", 1);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(0.0, distribution.getYMarginal(2), DELTA);
    }

    @Test
    public void testMutualInformation() {
        final Map<String, Integer> xValues
                = ImmutableMap.of("A", 1, "B", 2, "C", 0);
        final Map<String, Integer> yValues
                = ImmutableMap.of("A", 1, "B", 0, "C", 1);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(0.918296, distribution.getMutualInformation(),
                            DELTA);
    }
    
     @Test
    public void testMutualInformationSymmetric() {
        final Map<String, Integer> xValues
                = ImmutableMap.of("A", 1, "B", 2, "C", 0);
        final Map<String, Integer> yValues
                = ImmutableMap.of("A", 1, "B", 0, "C", 1);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);
        final JointProbabilityDistribution reversedDistribution
                = new JointProbabilityDistribution(yValues, xValues, 3);

        Assert.assertEquals(distribution.getMutualInformation(),
                            reversedDistribution.getMutualInformation(),
                            DELTA);
    }

    @Test
    public void testMutualInformationForNoEntropy() {
        final Map<String, Integer> xValues
                = ImmutableMap.of("A", 1, "B", 1, "C", 1);
        final Map<String, Integer> yValues
                = ImmutableMap.of("A", 2, "B", 2, "C", 2);

        final JointProbabilityDistribution distribution
                = new JointProbabilityDistribution(xValues, yValues, 3);

        Assert.assertEquals(0.0, distribution.getMutualInformation(),
                            DELTA);
    }

}
