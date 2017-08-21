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
package com.publictransitanalytics.scoregenerator.datalayer.distanceestimates;

import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.MapRangedStore;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.geography.Endpoints;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedEndpointDeterminer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
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
public class StoredDistanceEstimatorTest {

    @Test
    public void testReaches() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint point1 = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint point2 = new Geodetic2DPoint(
                new Longitude(-122.3, Longitude.DEGREES),
                new Latitude(47.6, Latitude.DEGREES));

        final Landmark location1 = new Landmark(sector, point1);
        final Landmark location2 = new Landmark(sector, point2);

        final RangedStore<LocationDistanceKey, String> candidateDistancesStore
                = new MapRangedStore<LocationDistanceKey, String>(
                        new TreeMap<>());
        final Store<LocationKey, Double> maxDistanceStore = new MapStore<>(
                new HashMap<>());
        final PreloadedEndpointDeterminer endpointDeterminer
                = new PreloadedEndpointDeterminer(
                        new Endpoints(location1.getLocation(),
                                      location2.getLocation()));

        final DistanceEstimator estimator = new StoredDistanceEstimator(
                Collections.singleton(location1), Collections.emptySet(),
                ImmutableSet.of(location1, location2), 6500, maxDistanceStore,
                candidateDistancesStore, endpointDeterminer);

        final Set<String> reachable = estimator.getReachableLocations(
                location1.getIdentifier(), 6500);
        Assert.assertEquals(1, reachable.size());
        Assert.assertEquals(ImmutableSet.of(location2.getIdentifier()),
                            reachable);
    }

    @Test
    public void testDoesNotUseSectorAsOrigin() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint point1 = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Landmark location1 = new Landmark(sector, point1);

        final RangedStore<LocationDistanceKey, String> candidateDistanceStore
                = new MapRangedStore<LocationDistanceKey, String>(
                        new TreeMap<>());
        final Store<LocationKey, Double> maxDistanceStore = new MapStore<>(
                new HashMap<>());
        final PreloadedEndpointDeterminer endpointDeterminer
                = new PreloadedEndpointDeterminer(
                        new Endpoints(location1.getLocation(),
                                      sector.getCanonicalPoint()));

        final DistanceEstimator estimator = new StoredDistanceEstimator(
                Collections.singleton(location1), ImmutableSet.of(sector),
                ImmutableSet.of(location1), 6500, maxDistanceStore,
                candidateDistanceStore, endpointDeterminer);

        final Set<String> reachable = estimator.getReachableLocations(
                sector.getIdentifier(), 6500);
        Assert.assertTrue(reachable.isEmpty());
    }

    @Test
    public void testDoesNotReach() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint point1 = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint point2 = new Geodetic2DPoint(
                new Longitude(-122.3, Longitude.DEGREES),
                new Latitude(47.6, Latitude.DEGREES));

        final Landmark location1 = new Landmark(sector, point1);
        final Landmark location2 = new Landmark(sector, point2);

        final RangedStore<LocationDistanceKey, String> candidateDistanceStore
                = new MapRangedStore<LocationDistanceKey, String>(
                        new TreeMap<>());
        final Store<LocationKey, Double> maxDistanceStore = new MapStore<>(
                new HashMap<>());
        final PreloadedEndpointDeterminer endpointDeterminer
                = new PreloadedEndpointDeterminer(new Endpoints(
                        location1.getLocation(), location2.getLocation()));
        final DistanceEstimator estimator = new StoredDistanceEstimator(
                Collections.singleton(location1), Collections.emptySet(),
                ImmutableSet.of(location1, location2), 6500, maxDistanceStore,
                candidateDistanceStore, endpointDeterminer);

        final Set<String> reachable = estimator.getReachableLocations(
                location1.getIdentifier(), 5000);
        Assert.assertEquals(0, reachable.size());
    }

    @Test
    public void testDoesNotGoBeyondMax() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));

        final Geodetic2DPoint point1 = new Geodetic2DPoint(
                new Longitude(-122.3236954, Longitude.DEGREES),
                new Latitude(47.6546556, Latitude.DEGREES));

        final Geodetic2DPoint point2 = new Geodetic2DPoint(
                new Longitude(-122.3, Longitude.DEGREES),
                new Latitude(47.6, Latitude.DEGREES));

        final Landmark location1 = new Landmark(sector, point1);
        final Landmark location2 = new Landmark(sector, point2);

        final RangedStore<LocationDistanceKey, String> store
                = new MapRangedStore<LocationDistanceKey, String>(
                        new TreeMap<>());
        final Store<LocationKey, Double> maxDistanceStore = new MapStore<>(
                new HashMap<>());
        final PreloadedEndpointDeterminer endpointDeterminer
                = new PreloadedEndpointDeterminer(new Endpoints(
                        location1.getLocation(), location2.getLocation()));

        final DistanceEstimator estimator = new StoredDistanceEstimator(
                Collections.singleton(location1), Collections.emptySet(),
                ImmutableSet.of(location1, location2), 4000, maxDistanceStore,
                store, endpointDeterminer);
        try {
            estimator.getReachableLocations(location1.getIdentifier(), 5000);
            Assert.fail();
        } catch (final ScoreGeneratorFatalException e) {
        }
    }
}
