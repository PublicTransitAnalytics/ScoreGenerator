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
package com.bitvantage.seattletransitisochrone.testhelpers;

import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.DistanceEstimator;
import com.google.common.collect.ImmutableSet;
import java.util.NavigableMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedDistanceEstimator implements DistanceEstimator {
    
    private final NavigableMap<Double, String> estimates;

    @Override
    public Set<String> getReachableLocations(final String originStopId,
                                             final double distanceMeters) {
        return ImmutableSet.copyOf(
                estimates.headMap(distanceMeters, true).values());
    }
    
}
