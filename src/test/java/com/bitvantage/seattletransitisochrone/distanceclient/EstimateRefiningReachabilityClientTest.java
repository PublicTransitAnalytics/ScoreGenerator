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
package com.bitvantage.seattletransitisochrone.distanceclient;

import com.publictransitanalytics.scoregenerator.distanceclient.EstimateRefiningReachabilityClient;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceFilter;
import com.publictransitanalytics.scoregenerator.datalayer.distanceestimates.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedDistanceEstimator;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedDistanceFilter;
import com.bitvantage.seattletransitisochrone.testhelpers.PreloadedTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;
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
public class EstimateRefiningReachabilityClientTest {

    private static final String LOCATION_ID_1
            = "(122° 19' 55\" W, 47° 37' 4\" N)";

    private final static Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));

    private final static Geodetic2DPoint POINT = new Geodetic2DPoint(
            new Longitude(-122.3236954, Longitude.DEGREES),
            new Latitude(47.6546556, Latitude.DEGREES));

    private final Landmark LOCATION_1 = new Landmark(SECTOR, POINT);

    private final Landmark HOME
            = new Landmark(
                    SECTOR, new Geodetic2DPoint(
                            new Longitude(-122.3320205, Longitude.DEGREES),
                            new Latitude(47.6178533, Latitude.DEGREES)));

    @Test
    public void testIgnoresOutOfEstimateRange() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                new TreeMap<>(ImmutableMap.<Double, String>of(
                        1000.0, LOCATION_ID_1)));

        final DistanceFilter filter = new PreloadedDistanceFilter(
                new TreeMap<>(ImmutableMap.<Duration, Landmark>of(
                        Duration.ZERO, LOCATION_1)), Duration.ofMinutes(2));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final ImmutableBiMap locationMap
                = ImmutableBiMap.of(LOCATION_ID_1, LOCATION_1);

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1, locationMap);

        final Map<VisitableLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME,
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertTrue(reachable.isEmpty());
    }

    @Test
    public void testIgnoresUnreachable() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                new TreeMap<>(ImmutableMap.<Double, String>of(
                        0.5, LOCATION_ID_1)));

        final DistanceFilter filter = new PreloadedDistanceFilter(
                new TreeMap<>(ImmutableMap.<Duration, Landmark>of(
                        Duration.ofMinutes(3), LOCATION_1)),
                Duration.ofMinutes(2));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final ImmutableBiMap locationMap
                = ImmutableBiMap.of(LOCATION_ID_1, LOCATION_1);

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1, locationMap);

        final Map<VisitableLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME,
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertTrue(reachable.isEmpty());
    }

    @Test
    public void testReturnsReachableEstimate() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                new TreeMap<>(ImmutableMap.<Double, String>of(
                        0.5, LOCATION_ID_1)));

        final DistanceFilter filter = new PreloadedDistanceFilter(
                new TreeMap<>(ImmutableMap.<Duration, Landmark>of(
                        Duration.ofMinutes(1), LOCATION_1)),
                Duration.ofMinutes(2));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final ImmutableBiMap locationMap
                = ImmutableBiMap.of(LOCATION_ID_1, LOCATION_1);

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1, locationMap);

        final Map<VisitableLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME,
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertEquals(1, reachable.size());
    }

    @Test
    public void testIgnoresUnmapped() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                new TreeMap<>(ImmutableMap.<Double, String>of(
                        0.5, LOCATION_ID_1)));

        final DistanceFilter filter = new PreloadedDistanceFilter(
                new TreeMap<>(ImmutableMap.<Duration, Landmark>of(
                        Duration.ofMinutes(1), LOCATION_1)),
                Duration.ofMinutes(2));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final ImmutableBiMap locationMap = ImmutableBiMap.of();

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1, locationMap);

        final Map<VisitableLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME,
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertTrue(reachable.isEmpty());
    }

}
