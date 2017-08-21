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
package com.publictransitanalytics.scoregenerator.geography;

import com.publictransitanalytics.scoregenerator.location.VisitableLocation;

/**
 * A component for choosing the distance points to use for potentially non-point
 * locations.
 * 
 * @author Public Transit Analytics
 */
public interface EndpointDeterminer {

    Endpoints getEndpoints(final VisitableLocation firstLocation,
                           final VisitableLocation secondLocation);
    
}
