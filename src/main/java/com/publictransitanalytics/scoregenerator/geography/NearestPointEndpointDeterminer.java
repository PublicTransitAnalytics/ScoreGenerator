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
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * Attempts to figure out the best two endpoints to use between location, 
 * because not all locations are points. Just gets the two closest points with
 * no respect to whether they are on water or off road.
 * @author Public Transit Analytics
 */
public class NearestPointEndpointDeterminer implements EndpointDeterminer {

    @Override
    public Endpoints getEndpoints(final VisitableLocation firstLocation,
                                  final VisitableLocation secondLocation) {
        final Geodetic2DPoint firstEndpoint = firstLocation.getNearestPoint(
                secondLocation.getCanonicalPoint());
        final Geodetic2DPoint secondEndpoint
                = secondLocation.getNearestPoint(
                        firstLocation.getCanonicalPoint());
        return new Endpoints(firstEndpoint, secondEndpoint);
    }

}
