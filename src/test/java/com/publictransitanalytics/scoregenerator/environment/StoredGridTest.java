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
package com.publictransitanalytics.scoregenerator.environment;

import com.bitvantage.bitvantagecaching.mocks.MapRangedStore;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridInfo;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridPointAssociation;
import com.publictransitanalytics.scoregenerator.datalayer.environment.SectorInfo;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridIdKey;
import com.publictransitanalytics.scoregenerator.datalayer.environment.GridPointAssociationKey;
import com.publictransitanalytics.scoregenerator.datalayer.environment.SectorKey;
import com.publictransitanalytics.scoregenerator.testhelpers.AllInEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.output.MapGenerator;
import com.publictransitanalytics.scoregenerator.testhelpers.KnockoutEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedSegmentFinder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class StoredGridTest {

    private static final GeoBounds SEATTLE_BOUNDS
            = new GeoBounds(
                    new GeoLongitude(
                            "-122.45970", AngleUnit.DEGREES),
                    new GeoLatitude(
                            "47.48172", AngleUnit.DEGREES),
                    new GeoLongitude(
                            "-122.22443", AngleUnit.DEGREES),
                    new GeoLatitude(
                            "47.734145", AngleUnit.DEGREES));

    private static final GeoBounds SYNTHETIC_BOUNDS
            = new GeoBounds(
                    new GeoLongitude(
                            "-130", AngleUnit.DEGREES),
                    new GeoLatitude(
                            "40", AngleUnit.DEGREES),
                    new GeoLongitude(
                            "-100", AngleUnit.DEGREES),
                    new GeoLatitude(
                            "60", AngleUnit.DEGREES));

    private static StoredGrid makeSyntheticGrid() throws InterruptedException {
        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.emptySet()),
                SYNTHETIC_BOUNDS, 3, 4, new AllInEnvironmentDetector(),
                new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        return grid;
    }

    @Test
    public void testReturnsCornerSector() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-100, AngleUnit.DEGREES),
                        new GeoLatitude(40, AngleUnit.DEGREES))));

        Assert.assertEquals(1, sectors.size());
        Assert.assertEquals(new GeoBounds(
                new GeoLongitude(-110, AngleUnit.DEGREES),
                new GeoLatitude(50, AngleUnit.DEGREES),
                new GeoLongitude(-100, AngleUnit.DEGREES),
                new GeoLatitude(40, AngleUnit.DEGREES)),
                            sectors.iterator().next().getBounds());
    }

    @Test
    public void testReturnsInteriorSector() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-111, AngleUnit.DEGREES),
                        new GeoLatitude(41, AngleUnit.DEGREES))));

        Assert.assertEquals(new GeoBounds(
                new GeoLongitude(-120, AngleUnit.DEGREES),
                new GeoLatitude(50, AngleUnit.DEGREES),
                new GeoLongitude(-110, AngleUnit.DEGREES),
                new GeoLatitude(40, AngleUnit.DEGREES)),
                            sectors.iterator().next().getBounds());
    }

    @Test
    public void testReturnsGreatestSector() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-129.95, AngleUnit.DEGREES),
                        new GeoLatitude(59, AngleUnit.DEGREES))));

        Assert.assertEquals(new GeoBounds(
                new GeoLongitude(-130, AngleUnit.DEGREES),
                new GeoLatitude(60, AngleUnit.DEGREES),
                new GeoLongitude(-120, AngleUnit.DEGREES),
                new GeoLatitude(50, AngleUnit.DEGREES)),
                            sectors.iterator().next().getBounds());
    }

    @Test
    public void testNullOnLowerLatitude() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-111, AngleUnit.DEGREES),
                        new GeoLatitude(35, AngleUnit.DEGREES))));

        Assert.assertTrue(sectors.isEmpty());
    }

    @Test
    public void testNullOnHigherLatitude() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-111, AngleUnit.DEGREES),
                        new GeoLatitude(65, AngleUnit.DEGREES))));

        Assert.assertTrue(sectors.isEmpty());
    }

    @Test
    public void testNullOnLowerLongitude() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-95, AngleUnit.DEGREES),
                        new GeoLatitude(45, AngleUnit.DEGREES))));

        Assert.assertTrue(sectors.isEmpty());
    }

    @Test
    public void testNullOnHigherLongitude() throws Exception {

        final Set<Sector> sectors = makeSyntheticGrid().getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-135, AngleUnit.DEGREES),
                        new GeoLatitude(45, AngleUnit.DEGREES))));

        Assert.assertTrue(sectors.isEmpty());
    }

    @Test
    public void testNullOnOutOfBounds() throws Exception {
        final GeoBounds outOfBounds = new GeoBounds(
                new GeoLongitude("-120", AngleUnit.DEGREES),
                new GeoLatitude("50", AngleUnit.DEGREES),
                new GeoLongitude("-110", AngleUnit.DEGREES),
                new GeoLatitude("60", AngleUnit.DEGREES));
        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.emptySet()),
                SYNTHETIC_BOUNDS, 3, 4, new KnockoutEnvironmentDetector(
                        Collections.singleton(outOfBounds)),
                new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));

        final Set<Sector> sectors = grid.getSectors(
                new Landmark(new GeoPoint(
                        new GeoLongitude(-115, AngleUnit.DEGREES),
                        new GeoLatitude(55, AngleUnit.DEGREES))));

        Assert.assertTrue(sectors.isEmpty());
    }

    @Test
    public void testMakesAllSectors() throws Exception {
        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.emptySet()),
                SEATTLE_BOUNDS, 3, 3, new AllInEnvironmentDetector(),
                new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));

        final Set<Sector> sectors = grid.getAllSectors();
        Assert.assertEquals(4, sectors.size());
    }

    @Test
    public void makesLatitudeGridpoint() throws Exception {
        final Segment oneCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30405", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.27010", AngleUnit.DEGREES),
                        new GeoLatitude("47.524541", AngleUnit.DEGREES)), 0, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.singleton(
                        oneCrossingSegment)), SEATTLE_BOUNDS, 3, 3,
                new AllInEnvironmentDetector(), new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(
                        new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        Assert.assertEquals(1, grid.getGridPoints().size());
    }

    @Test
    public void mapsLatitudeGridpointToTwoSectors() throws Exception {
        final Segment oneCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.27070", AngleUnit.DEGREES),
                        new GeoLatitude("47.524541", AngleUnit.DEGREES)), 0, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.singleton(
                        oneCrossingSegment)), SEATTLE_BOUNDS, 3, 3,
                new AllInEnvironmentDetector(), new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        final PointLocation gridPoint = grid.getGridPoints().iterator().next();
        final Set<Sector> relevantSectors = grid.getSectors(gridPoint);
        Assert.assertEquals(2, relevantSectors.size());
    }

    @Test
    public void makesLongitudeGridpoint() throws Exception {
        final Segment oneCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.38776", AngleUnit.DEGREES),
                        new GeoLatitude("47.693478", AngleUnit.DEGREES)), 0, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.singleton(
                        oneCrossingSegment)), SEATTLE_BOUNDS, 3, 3,
                new AllInEnvironmentDetector(), new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        Assert.assertEquals(1, grid.getGridPoints().size());
    }

    @Test
    public void mapsLongitudeGridpointToTwoSectors() throws Exception {
        final Segment oneCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.38776", AngleUnit.DEGREES),
                        new GeoLatitude("47.693478", AngleUnit.DEGREES)), 0, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.singleton(
                        oneCrossingSegment)), SEATTLE_BOUNDS, 3, 3,
                new AllInEnvironmentDetector(), new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        final PointLocation gridPoint = grid.getGridPoints().iterator().next();
        final Set<Sector> relevantSectors = grid.getSectors(gridPoint);
        Assert.assertEquals(2, relevantSectors.size());
    }

    @Test
    public void makesGridpoints() throws Exception {
        final Segment twoCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.37627", AngleUnit.DEGREES),
                        new GeoLatitude("47.555140", AngleUnit.DEGREES)), 0, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.singleton(
                        twoCrossingSegment)), SEATTLE_BOUNDS, 3, 3,
                new AllInEnvironmentDetector(), new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        Assert.assertEquals(2, grid.getGridPoints().size());
    }

    @Test
    public void mapsSectorsForMultipleGridpoints() throws Exception {
        final Segment twoCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.304055", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.37627", AngleUnit.DEGREES),
                        new GeoLatitude("47.555140", AngleUnit.DEGREES)), 0, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(Collections.singleton(
                        twoCrossingSegment)), SEATTLE_BOUNDS, 3, 3,
                new AllInEnvironmentDetector(), new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));
        final Set<Sector> relevantSectors = grid.getGridPoints().stream().map(
                point -> grid.getSectors(point)).flatMap(Set::stream).collect(
                Collectors.toSet());
        Assert.assertEquals(3, relevantSectors.size());
    }

    @Test
    public void makesAllSegmentGridpoints() throws Exception {
        final Segment oneCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.38776", AngleUnit.DEGREES),
                        new GeoLatitude("47.693478", AngleUnit.DEGREES)), 0, 0);
        final Segment twoCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.376270", AngleUnit.DEGREES),
                        new GeoLatitude("47.555140", AngleUnit.DEGREES)), 1, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(ImmutableSet.of(oneCrossingSegment,
                                                           twoCrossingSegment)),
                SEATTLE_BOUNDS, 3, 3, new AllInEnvironmentDetector(),
                new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));

        Assert.assertEquals(3, grid.getGridPoints().size());
    }

    @Test
    public void mapsSectorsForAllSegmentGridpoints() throws Exception {
        final Segment oneCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.30406", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.27070", AngleUnit.DEGREES),
                        new GeoLatitude("47.524541", AngleUnit.DEGREES)), 0, 0);
        final Segment twoCrossingSegment = new Segment(
                new GeoPoint(
                        new GeoLongitude("-122.304055", AngleUnit.DEGREES),
                        new GeoLatitude("47.719287", AngleUnit.DEGREES)),
                new GeoPoint(
                        new GeoLongitude("-122.37627", AngleUnit.DEGREES),
                        new GeoLatitude("47.555140", AngleUnit.DEGREES)), 1, 0);

        final StoredGrid grid = new StoredGrid(
                new PreloadedSegmentFinder(ImmutableSet.of(oneCrossingSegment,
                                                           twoCrossingSegment)),
                SEATTLE_BOUNDS, 3, 3, new AllInEnvironmentDetector(),
                new MapStore<>(new HashMap<>()),
                new MapRangedStore<SectorKey, SectorInfo>(new TreeMap<>()),
                new MapRangedStore<GridPointAssociationKey, GridPointAssociation>(
                        new TreeMap<>()));

        final Set<Sector> relevantSectors = grid.getGridPoints().stream().map(
                point -> grid.getSectors(point)).flatMap(Set::stream).collect(
                Collectors.toSet());
        Assert.assertEquals(4, relevantSectors.size());
    }

}
