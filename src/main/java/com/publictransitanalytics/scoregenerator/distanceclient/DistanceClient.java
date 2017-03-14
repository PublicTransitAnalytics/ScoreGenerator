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
package com.publictransitanalytics.scoregenerator.distanceclient;

import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.Table;
import java.util.Set;

/**
 * Gets the costs of walking of the product of sets of origins and destinations.
 *
 * @author Public Transit Analytics
 */
public interface DistanceClient {

    Table<VisitableLocation, VisitableLocation, WalkingCosts> getDistances(
            final Set<VisitableLocation> origins,
            final Set<VisitableLocation> destinations)
            throws DistanceClientException;

}
