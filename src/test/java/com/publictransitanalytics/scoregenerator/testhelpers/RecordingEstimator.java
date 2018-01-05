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
package com.publictransitanalytics.scoregenerator.testhelpers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.distanceclient.CalculatingDistanceEstimator;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.util.Set;
import lombok.Getter;

/**
 *
 * @author Public Transit Analytics
 */
public class RecordingEstimator implements CalculatingDistanceEstimator {

    @Getter
    final SetMultimap<PointLocation, PointLocation> record;

    public RecordingEstimator() {
        record = HashMultimap.create();
    }
    
    @Override
    public Set<PointLocation> getReachableLocations(PointLocation origin,
                                                        double distanceMeters)
            throws InterruptedException {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public void generateEstimate(final PointLocation origin,
                                 final PointLocation destination) {
        record.put(origin, destination);
    }
    
}
