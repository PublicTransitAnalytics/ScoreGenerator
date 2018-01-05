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

import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.distanceclient.types.DistanceCacheKey;
import com.publictransitanalytics.scoregenerator.distanceclient.types.WalkingDistanceMeasurement;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedDistanceClient;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class CachingDistanceClientTest {

    private static final GeoPoint ORIGIN_POINT = new GeoPoint(
            new GeoLongitude("-122.32370",
                                    AngleUnit.DEGREES),
            new GeoLatitude("47.654656",
                                   AngleUnit.DEGREES));

    private static final GeoPoint DESTINATION_POINT
            = new GeoPoint(
                    new GeoLongitude("-122.32899",
                                            AngleUnit.DEGREES),
                    new GeoLatitude("47.664286",
                                           AngleUnit.DEGREES));

    @Test
    public void testHandlesAbsence() throws Exception {

        final Landmark origin = new Landmark(DESTINATION_POINT);
        final Landmark destination = new Landmark(ORIGIN_POINT);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(Collections.emptyMap());
        final DistanceClient backingClient = new PreloadedDistanceClient(
                Collections.emptyMap());
        final DistanceClient client = new CachingDistanceClient(
                (point, consideredPoint) -> new ForwardPointOrderer(
                        point, consideredPoint), cache, backingClient);

        Assert.assertTrue(client.getDistances(
                origin, Collections.singleton(destination)).isEmpty());
    }

    @Test
    public void testReadsFromCache() throws Exception {
        final Landmark origin = new Landmark(DESTINATION_POINT);
        final Landmark destination = new Landmark(ORIGIN_POINT);
        final WalkingDistanceMeasurement measurement
                = new WalkingDistanceMeasurement(Duration.ZERO, 0);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(ImmutableMap.of(new DistanceCacheKey(
                        origin.getIdentifier(),
                        destination.getIdentifier()).getKeyString(),
                                                 measurement));
        final DistanceClient backingClient = new PreloadedDistanceClient(
                Collections.emptyMap());
        final DistanceClient client = new CachingDistanceClient(
                (point, consideredPoint) -> new ForwardPointOrderer(
                        point, consideredPoint), cache, backingClient);

        final Map<PointLocation, WalkingCosts> distances
                = client.getDistances(origin,
                                      Collections.singleton(destination));
        Assert.assertEquals(1, distances.size());
        final WalkingCosts entry = distances.get(destination);
        final WalkingCosts costs = new WalkingCosts(Duration.ZERO, 0);
        Assert.assertEquals(costs, entry);
    }

    @Test
    public void testFallsBackToClient() throws Exception {
        final Landmark origin = new Landmark(DESTINATION_POINT);
        final Landmark destination = new Landmark(ORIGIN_POINT);
        final WalkingCosts costs = new WalkingCosts(Duration.ZERO, 0);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(new HashMap<>());
        final DistanceClient backingClient = new PreloadedDistanceClient(
                ImmutableMap.<PointLocation, WalkingCosts>of(
                        destination, costs));
        final DistanceClient client = new CachingDistanceClient(
                (point, consideredPoint) -> new ForwardPointOrderer(
                        point, consideredPoint), cache, backingClient);

        final Map<PointLocation, WalkingCosts> distances
                = client.getDistances(origin,
                                      Collections.singleton(destination));
        Assert.assertEquals(1, distances.size());
        final WalkingCosts entry = distances.get(destination);
        Assert.assertEquals(costs, entry);
    }

    @Test
    public void testPopulatesCache() throws Exception {
        final Landmark origin = new Landmark(DESTINATION_POINT);
        final Landmark destination = new Landmark(ORIGIN_POINT);
        final WalkingCosts costs = new WalkingCosts(Duration.ZERO, 0);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(new HashMap<>());
        final DistanceClient backingClient = new PreloadedDistanceClient(
                ImmutableMap.<PointLocation, WalkingCosts>of(
                        destination, costs));
        final DistanceClient client = new CachingDistanceClient(
                (point, consideredPoint) -> new ForwardPointOrderer(
                        point, consideredPoint), cache, backingClient);

        client.getDistances(origin,
                            Collections.singleton(destination));

        final DistanceCacheKey cacheKey = new DistanceCacheKey(
                origin.getIdentifier(), destination.getIdentifier());
        final WalkingDistanceMeasurement measurement
                = new WalkingDistanceMeasurement(Duration.ZERO, 0);
        Assert.assertEquals(measurement, cache.get(cacheKey));
    }

}
