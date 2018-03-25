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

import com.bitvantage.bitvantagecaching.RangedKeyStore;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.SetRangedKeyStore;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationKey;
import com.publictransitanalytics.scoregenerator.datalayer.distance.LocationTimeKey;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedDistanceClient;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class RangedCachingReachabilityClientTest {

    private static final Landmark LOCATION_1 = new Landmark(new GeoPoint(
            new GeoLongitude("-123", AngleUnit.DEGREES),
            new GeoLatitude("46", AngleUnit.DEGREES)));
    private static final String LOCATION_1_ID = LOCATION_1.getIdentifier();

    private static final Landmark LOCATION_2 = new Landmark(new GeoPoint(
            new GeoLongitude("-122", AngleUnit.DEGREES),
            new GeoLatitude("47", AngleUnit.DEGREES)));
    private static final String LOCATION_2_ID = LOCATION_2.getIdentifier();

    private static final Landmark LOCATION_3 = new Landmark(new GeoPoint(
            new GeoLongitude("-121", AngleUnit.DEGREES),
            new GeoLatitude("48", AngleUnit.DEGREES)));
    private static final String LOCATION_3_ID = LOCATION_3.getIdentifier();

    private static final BiMap<String, PointLocation> POINT_ID_MAP
            = ImmutableBiMap.of(LOCATION_1_ID, LOCATION_1,
                                LOCATION_2_ID, LOCATION_2,
                                LOCATION_3_ID, LOCATION_3);

    @Test
    public void testHasStoredDistances() throws Exception {
        final RangedKeyStore<LocationTimeKey> timeStore
                = new SetRangedKeyStore<>(ImmutableSortedSet.of(
                        LocationTimeKey.getWriteKey(
                                LOCATION_1_ID, 60, LOCATION_2_ID)));
        final Store<LocationKey, Integer> maxTimeStore = new MapStore<>(
                ImmutableMap.of(new LocationKey(LOCATION_1_ID).getKeyString(),
                                60));
        final TimeTracker tracker = new ForwardTimeTracker();

        final ReachabilityClient client = new RangedCachingReachabilityClient(
                timeStore, maxTimeStore, tracker, null, null, POINT_ID_MAP);
        final Map<PointLocation, WalkingCosts> result = client.getWalkingCosts(
                LOCATION_1, LocalDateTime.of(1987, Month.MARCH, 8, 4, 39),
                LocalDateTime.of(1987, Month.MARCH, 8, 4, 40));

        Assert.assertEquals(ImmutableMap.<PointLocation, WalkingCosts>of(
                LOCATION_2, new WalkingCosts(Duration.ofMinutes(1), -1.0)),
                            result);
    }

    @Test
    public void testHasNoStoredDistances() throws Exception {
        final NavigableSet<LocationTimeKey> timeStoreSet = new TreeSet<>();
        final RangedKeyStore<LocationTimeKey> timeStore
                = new SetRangedKeyStore<>(timeStoreSet);

        final Map<String, Integer> maxTimestoreMap = new HashMap<>();
        final Store<LocationKey, Integer> maxTimeStore
                = new MapStore<>(maxTimestoreMap);
        final TimeTracker tracker = new ForwardTimeTracker();

        final DistanceClient distanceClient = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                60), -1.0)));
        final DistanceClient distanceEstimator = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                60), -1.0)));

        final ReachabilityClient client = new RangedCachingReachabilityClient(
                timeStore, maxTimeStore, tracker, distanceClient,
                distanceEstimator, POINT_ID_MAP);
        final Map<PointLocation, WalkingCosts> result = client.getWalkingCosts(
                LOCATION_1, LocalDateTime.of(1987, Month.MARCH, 8, 4, 39),
                LocalDateTime.of(1987, Month.MARCH, 8, 4, 40));
        Assert.assertEquals(ImmutableMap.<PointLocation, WalkingCosts>of(
                LOCATION_3, new WalkingCosts(Duration.ofSeconds(60), -1.0)),
                            result);

        final String location1KeyString
                = new LocationKey(LOCATION_1_ID).getKeyString();
        Assert.assertEquals(60,
                            maxTimestoreMap.get(location1KeyString).intValue());
        Assert.assertEquals(1, timeStoreSet.size());
        final LocationTimeKey newKey = LocationTimeKey.getWriteKey(
                LOCATION_1_ID, 60, LOCATION_3_ID);
        Assert.assertTrue(timeStoreSet.contains(newKey));
    }

    @Test
    public void testHasSomeStoredDistances() throws Exception {
        final NavigableSet<LocationTimeKey> timeStoreSet = new TreeSet<>();
        final LocationTimeKey location1TimeKey = LocationTimeKey.getWriteKey(
                LOCATION_1_ID, 30, LOCATION_2_ID);
        timeStoreSet.add(location1TimeKey);
        final RangedKeyStore<LocationTimeKey> timeStore 
                = new SetRangedKeyStore<>(timeStoreSet);

        final Map<String, Integer> maxTimestoreMap = new HashMap<>();
        final String location1KeyString
                = new LocationKey(LOCATION_1_ID).getKeyString();
        maxTimestoreMap.put(location1KeyString, 30);
        final Store<LocationKey, Integer> maxTimeStore
                = new MapStore<>(maxTimestoreMap);
        final TimeTracker tracker = new ForwardTimeTracker();

        final DistanceClient distanceClient = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                60), -1.0)));
        final DistanceClient distanceEstimator = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                60), -1.0)));

        final ReachabilityClient client = new RangedCachingReachabilityClient(
                timeStore, maxTimeStore, tracker, distanceClient,
                distanceEstimator, POINT_ID_MAP);
        final Map<PointLocation, WalkingCosts> result = client.getWalkingCosts(
                LOCATION_1, LocalDateTime.of(1987, Month.MARCH, 8, 4, 39),
                LocalDateTime.of(1987, Month.MARCH, 8, 4, 40));

        Assert.assertEquals(ImmutableMap.<PointLocation, WalkingCosts>of(
                LOCATION_2, new WalkingCosts(Duration.ofSeconds(30), -1.0),
                LOCATION_3, new WalkingCosts(Duration.ofSeconds(60), -1.0)),
                            result);
        Assert.assertEquals(60,
                            maxTimestoreMap.get(location1KeyString).intValue());
        Assert.assertEquals(2, timeStoreSet.size());
        Assert.assertTrue(timeStoreSet.contains(location1TimeKey));
        final LocationTimeKey newKey = LocationTimeKey.getWriteKey(
                LOCATION_1_ID, 60, LOCATION_3_ID);
        Assert.assertTrue(timeStoreSet.contains(newKey));
    }

    @Test
    public void testNoNewDistances() throws Exception {
        final NavigableSet<LocationTimeKey> timeStoreSet
                = new TreeSet<>();
        final LocationTimeKey location1TimeKey = LocationTimeKey.getWriteKey(
                LOCATION_1_ID, 30, LOCATION_2_ID);
        timeStoreSet.add(location1TimeKey);
        final RangedKeyStore<LocationTimeKey> timeStore
                = new SetRangedKeyStore<>(timeStoreSet);

        final Map<String, Integer> maxTimestoreMap = new HashMap<>();
        final String location1KeyString
                = new LocationKey(LOCATION_1_ID).getKeyString();
        maxTimestoreMap.put(location1KeyString, 30);
        final Store<LocationKey, Integer> maxTimeStore
                = new MapStore<>(maxTimestoreMap);
        final TimeTracker tracker = new ForwardTimeTracker();

        final DistanceClient distanceClient = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                60), -1.0)));
        final DistanceClient distanceEstimator = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                75), -1.0)));

        final ReachabilityClient client = new RangedCachingReachabilityClient(
                timeStore, maxTimeStore, tracker, distanceClient,
                distanceEstimator, POINT_ID_MAP);
        final Map<PointLocation, WalkingCosts> result = client.getWalkingCosts(
                LOCATION_1, LocalDateTime.of(1987, Month.MARCH, 8, 4, 39),
                LocalDateTime.of(1987, Month.MARCH, 8, 4, 40));

        Assert.assertEquals(ImmutableMap.<PointLocation, WalkingCosts>of(
                LOCATION_2, new WalkingCosts(Duration.ofSeconds(30), -1.0)),
                            result);
        Assert.assertEquals(60,
                            maxTimestoreMap.get(location1KeyString).intValue());
        Assert.assertEquals(1, timeStoreSet.size());
        Assert.assertTrue(timeStoreSet.contains(location1TimeKey));
    }

    @Test
    public void testEstimateShortCircuitsDistance() throws Exception {
        final NavigableSet<LocationTimeKey> timeStoreSet
                = new TreeSet<>();
        final LocationTimeKey location1TimeKey = LocationTimeKey.getWriteKey(
                LOCATION_1_ID, 30, LOCATION_2_ID);
        timeStoreSet.add(location1TimeKey);
        final RangedKeyStore<LocationTimeKey> timeStore 
                = new SetRangedKeyStore<>(timeStoreSet);

        final Map<String, Integer> maxTimestoreMap = new HashMap<>();
        final String location1KeyString
                = new LocationKey(LOCATION_1_ID).getKeyString();
        maxTimestoreMap.put(location1KeyString, 30);
        final Store<LocationKey, Integer> maxTimeStore
                = new MapStore<>(maxTimestoreMap);
        final TimeTracker tracker = new ForwardTimeTracker();

        final DistanceClient distanceEstimator = new PreloadedDistanceClient(
                ImmutableMap.of(LOCATION_3, new WalkingCosts(Duration.ofSeconds(
                                75), -1.0)));

        final ReachabilityClient client = new RangedCachingReachabilityClient(
                timeStore, maxTimeStore, tracker, null, distanceEstimator,
                POINT_ID_MAP);
        final Map<PointLocation, WalkingCosts> result = client.getWalkingCosts(
                LOCATION_1, LocalDateTime.of(1987, Month.MARCH, 8, 4, 39),
                LocalDateTime.of(1987, Month.MARCH, 8, 4, 40));

        Assert.assertEquals(ImmutableMap.<PointLocation, WalkingCosts>of(
                LOCATION_2, new WalkingCosts(Duration.ofSeconds(30), -1.0)),
                            result);
        Assert.assertEquals(60,
                            maxTimestoreMap.get(location1KeyString).intValue());
        Assert.assertEquals(1, timeStoreSet.size());
        Assert.assertTrue(timeStoreSet.contains(location1TimeKey));
    }

}
