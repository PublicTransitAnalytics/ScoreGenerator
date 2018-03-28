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
import java.time.Duration;
import java.util.Map;

/**
 *
 * @author Public Transit Analytics
 */
public interface DistanceStoreManager {

    Duration getMaxStored(PointLocation location) throws InterruptedException;

    void updateMaxStored(PointLocation location, Duration duration) 
            throws InterruptedException;

    Map<PointLocation, WalkingCosts> get(PointLocation location,
                                         Duration duration)
            throws InterruptedException;

    void putAll(PointLocation location, Map<PointLocation, WalkingCosts> costs)
            throws InterruptedException;

}
