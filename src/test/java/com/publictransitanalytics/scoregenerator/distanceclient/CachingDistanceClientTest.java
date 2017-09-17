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

import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClient;
import com.publictransitanalytics.scoregenerator.distanceclient.CachingDistanceClient;
import com.bitvantage.bitvantagecaching.Cache;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.MapCache;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.distanceclient.types.DistanceCacheKey;
import com.publictransitanalytics.scoregenerator.distanceclient.types.WalkingDistanceMeasurement;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedDistanceClient;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import edu.emory.mathcs.backport.java.util.Collections;
import java.time.Duration;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 *
 * @author Public Transit Analytics
 */
public class CachingDistanceClientTest {

    @Test
    public void testHandlesAbsence() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint originPoint = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint destinationPoint = new Geodetic2DPoint(
                new Longitude(-122.328989, Longitude.DEGREES),
                new Latitude(47.6642855, Latitude.DEGREES));

        final Landmark origin = new Landmark(sector, destinationPoint);
        final Landmark destination = new Landmark(sector, originPoint);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(Collections.emptyMap());
        final DistanceClient backingClient = new PreloadedDistanceClient(
                ImmutableTable
                        .<VisitableLocation, VisitableLocation, WalkingCosts>of());
        final DistanceClient client
                = new CachingDistanceClient(cache, backingClient);

        Assert.assertTrue(client.getDistances(
                Collections.singleton(origin),
                Collections.singleton(destination)).isEmpty());

    }

    @Test
    public void testReadsFromCache() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint originPoint = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint destinationPoint = new Geodetic2DPoint(
                new Longitude(-122.328989, Longitude.DEGREES),
                new Latitude(47.6642855, Latitude.DEGREES));

        final Landmark origin = new Landmark(sector, destinationPoint);
        final Landmark destination = new Landmark(sector, originPoint);
        final WalkingDistanceMeasurement measurement
                = new WalkingDistanceMeasurement(Duration.ZERO, 0);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(ImmutableMap.of(new DistanceCacheKey(
                        origin.getIdentifier(),
                        destination.getIdentifier()).getKeyString(),
                                                 measurement));
        final DistanceClient backingClient = new PreloadedDistanceClient(
                ImmutableTable
                        .<VisitableLocation, VisitableLocation, WalkingCosts>of());
        final DistanceClient client
                = new CachingDistanceClient(cache, backingClient);

        final Table<VisitableLocation, VisitableLocation, WalkingCosts> distances
                = client.getDistances(Collections.singleton(origin),
                                      Collections.singleton(destination));
        Assert.assertEquals(1, distances.size());
        final WalkingCosts entry = distances.get(origin, destination);
        final WalkingCosts costs = new WalkingCosts(Duration.ZERO, 0);
        Assert.assertEquals(costs, entry);
    }

    @Test
    public void testFallsBackToClient() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint originPoint = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint destinationPoint = new Geodetic2DPoint(
                new Longitude(-122.328989, Longitude.DEGREES),
                new Latitude(47.6642855, Latitude.DEGREES));

        final Landmark origin = new Landmark(sector, destinationPoint);
        final Landmark destination = new Landmark(sector, originPoint);
        final WalkingCosts costs = new WalkingCosts(Duration.ZERO, 0);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(new HashMap<>());
        final DistanceClient backingClient = new PreloadedDistanceClient(
                ImmutableTable
                        .<VisitableLocation, VisitableLocation, WalkingCosts>of(
                                origin, destination, costs));
        final DistanceClient client
                = new CachingDistanceClient(cache, backingClient);

        final Table<VisitableLocation, VisitableLocation, WalkingCosts> distances
                = client.getDistances(Collections.singleton(origin),
                                      Collections.singleton(destination));
        Assert.assertEquals(1, distances.size());
        final WalkingCosts entry = distances.get(origin, destination);
        Assert.assertEquals(costs, entry);
    }

    @Test
    public void testPopulatesCache() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint originPoint = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint destinationPoint = new Geodetic2DPoint(
                new Longitude(-122.328989, Longitude.DEGREES),
                new Latitude(47.6642855, Latitude.DEGREES));

        final Landmark origin = new Landmark(sector, destinationPoint);
        final Landmark destination = new Landmark(sector, originPoint);
        final WalkingCosts costs = new WalkingCosts(Duration.ZERO, 0);

        final Store<DistanceCacheKey, WalkingDistanceMeasurement> cache
                = new MapStore<>(new HashMap<>());
        final DistanceClient backingClient = new PreloadedDistanceClient(
                ImmutableTable
                        .<VisitableLocation, VisitableLocation, WalkingCosts>of(
                                origin, destination, costs));
        final DistanceClient client
                = new CachingDistanceClient(cache, backingClient);

        client.getDistances(Collections.singleton(origin),
                            Collections.singleton(destination));

        final DistanceCacheKey cacheKey = new DistanceCacheKey(
                origin.getIdentifier(), destination.getIdentifier());
        final WalkingDistanceMeasurement measurement
                = new WalkingDistanceMeasurement(Duration.ZERO, 0);
        Assert.assertEquals(measurement, cache.get(cacheKey));
    }

}
