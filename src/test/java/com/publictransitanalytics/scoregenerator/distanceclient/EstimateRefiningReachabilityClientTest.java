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

import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedDistanceEstimator;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedDistanceClient;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class EstimateRefiningReachabilityClientTest {

    private final static GeoPoint POINT = new GeoPoint(
            new GeoLongitude("-122.32370", AngleUnit.DEGREES),
            new GeoLatitude("47.654656", AngleUnit.DEGREES));

    private final Landmark LOCATION_1 = new Landmark(POINT);

    private final Landmark HOME = new Landmark(new GeoPoint(
            new GeoLongitude("-122.33202", AngleUnit.DEGREES),
            new GeoLatitude("47.617853", AngleUnit.DEGREES)));

    @Test
    public void testNoEstimates() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                ImmutableSortedMap.of());

        final DistanceClient filter = new PreloadedDistanceClient(
                ImmutableMap.<PointLocation, WalkingCosts>of(
                        LOCATION_1, 
                        new WalkingCosts(Duration.ofMinutes(2), 0)));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1);

        final Map<PointLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME, 
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertTrue(reachable.isEmpty());
    }

    @Test
    public void testIgnoresUnreachable() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                new TreeMap<>(ImmutableMap.<Double, PointLocation>of(
                        0.5, LOCATION_1)));

        final DistanceClient filter = new PreloadedDistanceClient(
                ImmutableMap.<PointLocation, WalkingCosts>of(
                        LOCATION_1,
                        new WalkingCosts(Duration.ofMinutes(3), 0)));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1);

        final Map<PointLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME,
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertTrue(reachable.isEmpty());
    }

    @Test
    public void testReturnsReachableEstimate() throws Exception {
        final DistanceEstimator estimator = new PreloadedDistanceEstimator(
                new TreeMap<>(ImmutableMap.<Double, PointLocation>of(
                        0.5, LOCATION_1)));

        final DistanceClient filter = new PreloadedDistanceClient(
                ImmutableMap.<PointLocation, WalkingCosts>of(
                        LOCATION_1,
                        new WalkingCosts(Duration.ofMinutes(1), 0)));

        final TimeTracker tracker = new PreloadedTimeTracker(
                null, true, Duration.ofMinutes(2));

        final EstimateRefiningReachabilityClient reachabilityClient
                = new EstimateRefiningReachabilityClient(
                        filter, estimator, tracker, 1);

        final Map<PointLocation, WalkingCosts> reachable
                = reachabilityClient.getWalkingDistances(
                        HOME, 
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 45),
                        LocalDateTime.of(2017, Month.FEBRUARY, 18, 16, 47));

        Assert.assertEquals(1, reachable.size());
    }

}
