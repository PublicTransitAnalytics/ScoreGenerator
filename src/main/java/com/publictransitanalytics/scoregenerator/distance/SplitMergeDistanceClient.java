/*
 * Copyright 2018 Public Transit Analytics.
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
package com.publictransitanalytics.scoregenerator.distance;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class SplitMergeDistanceClient implements DistanceClient {

    private final DistanceClient client;
    private final int maxConsidered;

    @Override
    public Map<PointLocation, WalkingCosts> getDistances(
            final PointLocation point,
            final Set<PointLocation> consideredPoints)
            throws DistanceClientException, InterruptedException {
        final List<PointLocation> consideredList
                = ImmutableList.copyOf(consideredPoints);
        int base = 0;
        final int total = consideredPoints.size();
        final ImmutableMap.Builder<PointLocation, WalkingCosts> resultBuilder = 
                ImmutableMap.builder();
        while(base < total) {
            int end = Math.min(base + maxConsidered, total);
            final Set<PointLocation> consideredSubset = ImmutableSet.copyOf(
                    consideredList.subList(base, end));
            final Map<PointLocation, WalkingCosts> distances 
                    = client.getDistances(point, consideredSubset);
            resultBuilder.putAll(distances);
            base = end;
        }
        return resultBuilder.build();
    }

    @Override
    public void close() {
    }

}
