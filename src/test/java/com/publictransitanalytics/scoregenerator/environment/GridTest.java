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

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoBounds;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import com.publictransitanalytics.scoregenerator.geography.AllInEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.output.MapGenerator;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GridTest {

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

    @Test
    public void testMakesAllSectors() throws Exception {
        final Grid grid = new Grid(Collections.emptySet(), SEATTLE_BOUNDS,
                                   3, 3, new AllInEnvironmentDetector());

        final Set<Sector> sectors = grid.getAllSectors();
        Assert.assertEquals(4, sectors.size());
    }

    @Test
    public void testMakesAllCenters() throws Exception {
        final Grid grid = new Grid(Collections.emptySet(), SEATTLE_BOUNDS,
                                   3, 3, new AllInEnvironmentDetector());

        final Set<PointLocation> sectors = grid.getCenters();
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

        final Grid grid = new Grid(Collections.singleton(oneCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
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

        final Grid grid = new Grid(Collections.singleton(oneCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
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

        final Grid grid = new Grid(Collections.singleton(oneCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
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

        final Grid grid = new Grid(Collections.singleton(oneCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
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

        final Grid grid = new Grid(Collections.singleton(twoCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
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

        final Grid grid = new Grid(Collections.singleton(twoCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
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

        final Grid grid = new Grid(ImmutableSet.of(oneCrossingSegment,
                                                   twoCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());
        MapGenerator generator = new MapGenerator();
        generator.makeEmptyMap(grid, grid.getGridPoints(), ImmutableSet.of(oneCrossingSegment, twoCrossingSegment), "blorfy");
        
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

        final Grid grid = new Grid(ImmutableSet.of(oneCrossingSegment,
                                                   twoCrossingSegment),
                                   SEATTLE_BOUNDS, 3, 3,
                                   new AllInEnvironmentDetector());

        final Set<Sector> relevantSectors = grid.getGridPoints().stream().map(
                point -> grid.getSectors(point)).flatMap(Set::stream).collect(
                Collectors.toSet());
        Assert.assertEquals(4, relevantSectors.size());
    }

}
