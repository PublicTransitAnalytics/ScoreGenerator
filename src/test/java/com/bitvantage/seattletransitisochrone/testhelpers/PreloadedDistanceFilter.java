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

import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClientException;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceFilter;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedDistanceFilter implements DistanceFilter {

    private final NavigableMap<Duration, VisitableLocation> map;
    private final Duration duration;

    @Override
    public Map<VisitableLocation, WalkingCosts> getFilteredDistances(
            final PointLocation center, final Set<VisitableLocation> candidates,
            final LocalDateTime currentTime, final LocalDateTime cutoffTime)
            throws DistanceClientException {

        final NavigableMap<Duration, VisitableLocation> headMap
                = map.headMap(duration, true);
        return headMap.entrySet().stream().filter(
                entry -> candidates.contains(entry.getValue()))
                .collect(Collectors.toMap(
                        entry -> entry.getValue(),
                        entry -> new WalkingCosts(
                                entry.getKey(), entry.getKey().toMinutes())));
    }

}
