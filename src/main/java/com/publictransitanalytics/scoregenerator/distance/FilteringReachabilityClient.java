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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class FilteringReachabilityClient implements ReachabilityClient {

    private final ReachabilityClient client;
    private final Set<? extends PointLocation> filtered;

    @Override
    public Map<PointLocation, WalkingCosts> getWalkingCosts(
            final PointLocation location, final LocalDateTime currentTime,
            final LocalDateTime cutoffTime)
            throws DistanceClientException, InterruptedException {
        return client.getWalkingCosts(location, currentTime, cutoffTime)
                .entrySet().stream()
                .filter(entry -> !filtered.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          Map.Entry::getValue));
    }
}
