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
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.publictransitanalytics.scoregenerator.geography.EndpointDeterminer;
import com.publictransitanalytics.scoregenerator.geography.Endpoints;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetectorException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * A DistanceClient that uses the GoogleDistanceMatrix API to get the distances
 * between the product of a set of origins and destinations.
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class GoogleDistanceClient implements DistanceClient {

    private final GeoApiContext context;
    private final WaterDetector waterDetector;
    private final EndpointDeterminer endpointDeterminer;
    private final PointOrdererFactory ordererFactory;

    public GoogleDistanceClient(final String key,
                                final WaterDetector waterDetector,
                                final EndpointDeterminer endpointDeterminer,
                                final PointOrdererFactory ordererFactory) {
        context = new GeoApiContext().setApiKey(key).setRetryTimeout(
                0, TimeUnit.SECONDS);
        this.waterDetector = waterDetector;
        this.endpointDeterminer = endpointDeterminer;
        this.ordererFactory = ordererFactory;
    }

    @Override
    public Map<VisitableLocation, WalkingCosts> getDistances(
            final VisitableLocation point,
            final Set<VisitableLocation> consideredPoints)
            throws DistanceClientException, InterruptedException {

        try {
            final ImmutableMap.Builder<VisitableLocation, WalkingCosts> resultBuilder
                    = ImmutableMap.builder();

            for (final VisitableLocation consideredPoint
                         : consideredPoints) {
                final PointOrderer orderer = ordererFactory.getOrderer(
                        point, consideredPoint);
                final VisitableLocation origin = orderer.getOrigin();
                final VisitableLocation destination = orderer.getDestination();

                final Endpoints endpoints
                        = endpointDeterminer.getEndpoints(origin, destination);
                final Geodetic2DPoint originPoint
                        = endpoints.getFirstEndpoint();
                final Geodetic2DPoint destinationPoint
                        = endpoints.getSecondEndpoint();

                if (!waterDetector.isOnWater(originPoint)
                            && !waterDetector.isOnWater(destinationPoint)) {

                    final LatLng originLatLng = new LatLng(
                            originPoint.getLatitudeAsDegrees(),
                            originPoint.getLongitudeAsDegrees());

                    final LatLng destinationLatLng = new LatLng(
                            destinationPoint.getLatitudeAsDegrees(),
                            destinationPoint.getLongitudeAsDegrees());

                    final DistanceMatrixApiRequest request
                            = DistanceMatrixApi
                                    .newRequest(context);

                    request.origins(originLatLng);
                    request.destinations(destinationLatLng);
                    request.mode(TravelMode.WALKING);
                    request.units(Unit.METRIC);
                    try {
                        final DistanceMatrix response = request.await();
                        final DistanceMatrixRow row = response.rows[0];
                        final DistanceMatrixElement element
                                = row.elements[0];

                        if (element.status
                                .equals(DistanceMatrixElementStatus.OK)) {
                            WalkingCosts measurement = new WalkingCosts(
                                    Duration.ofSeconds(
                                            element.duration.inSeconds),
                                    element.distance.inMeters);
                            resultBuilder.put(consideredPoint, measurement);
                        }
                    } catch (Exception e) {
                        throw new DistanceClientException(e);
                    }
                }
            }

            return resultBuilder.build();
        } catch (final WaterDetectorException e) {
            throw new DistanceClientException(e);
        }
    }

    @Override
    public void close() {
    }
}
