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

import com.google.common.collect.ImmutableMap;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.publictransitanalytics.scoregenerator.geography.EndpointDeterminer;
import com.publictransitanalytics.scoregenerator.geography.Endpoints;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * DistanceClient that uses Graphhopper.
 *
 * @author Public Transit Analytics
 */
@Slf4j
@RequiredArgsConstructor
public class GraphhopperLocalDistanceClient implements DistanceClient {

    private final PointOrdererFactory ordererFactory;
    private final GraphHopper hopper;
    private final EndpointDeterminer endpointDeterminer;
    private final WaterDetector waterDetector;

    @Override
    public Map<VisitableLocation, WalkingCosts> getDistances(
            final VisitableLocation point,
            final Set<VisitableLocation> consideredPoints)
            throws DistanceClientException, InterruptedException {

        final ImmutableMap.Builder<VisitableLocation, WalkingCosts> resultBuilder
                = ImmutableMap.builder();

        for (final VisitableLocation consideredPoint : consideredPoints) {
            final PointOrderer orderer = ordererFactory.getOrderer(
                    point, consideredPoint);
            final VisitableLocation origin = orderer.getOrigin();
            final VisitableLocation destination = orderer.getDestination();

            final Endpoints endpoints = endpointDeterminer.getEndpoints(
                    origin, destination);
            final Geodetic2DPoint originPoint
                    = endpoints.getFirstEndpoint();
            final Geodetic2DPoint destinationPoint
                    = endpoints.getSecondEndpoint();

            final GHRequest req = new GHRequest(
                    originPoint.getLatitudeAsDegrees(),
                    originPoint.getLongitudeAsDegrees(),
                    destinationPoint.getLatitudeAsDegrees(),
                    destinationPoint.getLongitudeAsDegrees())
                    .setWeighting("fastest")
                    .setVehicle("foot")
                    .setLocale(Locale.US);
            final GHResponse rsp = hopper.route(req);

            if (rsp.hasErrors()) {
                throw new DistanceClientException(
                        rsp.getErrors().toString());
            }

            final PathWrapper path = rsp.getBest();

            double distance = path.getDistance();
            long timeInMs = path.getTime();
            log.debug("Getting costs between {} and {}: {} m {} ms",
                      originPoint, destinationPoint, distance,
                      timeInMs);

            final WalkingCosts costs = new WalkingCosts(
                    Duration.ofMillis(timeInMs), distance);
            resultBuilder.put(consideredPoint, costs);
        }

        return resultBuilder.build();
    }

    @Override
    public void close() {
    }

}
