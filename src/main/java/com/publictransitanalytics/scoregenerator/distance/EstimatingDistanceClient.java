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

import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class EstimatingDistanceClient implements DistanceClient {

    private final double walkingMetersPerSecond;
    
    @Override
    public Map<PointLocation, WalkingCosts> getDistances(
            final PointLocation point,
            final Set<PointLocation> consideredPoints)
            throws DistanceClientException, InterruptedException {
        
        final GeoPoint location = point.getLocation();
        final ImmutableMap.Builder<PointLocation, WalkingCosts> builder 
                = ImmutableMap.builder();
        for (final PointLocation otherPoint : consideredPoints) {
            final GeoPoint otherLocation = otherPoint.getLocation();
            final double distanceMeters 
                    = location.getDistanceMeters(otherLocation);
            final Duration time = Duration.ofSeconds(
                    (long) Math.floor(distanceMeters / walkingMetersPerSecond));
            final WalkingCosts costs = new WalkingCosts(time, distanceMeters);
            builder.put(otherPoint, costs);
        }
        return builder.build();
    }

    @Override
    public void close() {
    }

}
