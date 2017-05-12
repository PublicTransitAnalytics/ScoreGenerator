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
package com.publictransitanalytics.scoregenerator.visitors;

import com.publictransitanalytics.scoregenerator.visitors.WalkVisitor;
import com.publictransitanalytics.scoregenerator.Mode;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.testhelpers.CountingVisitorFactory;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedReachabilityClient;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedTimeTracker;
import com.publictransitanalytics.scoregenerator.tracking.ForwardMovingPath;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.testhelpers.SerialWorkAllocator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
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
public class WalkVisitorTest {

    private static final MovementPath PATH
            = new ForwardMovingPath(ImmutableList.of());

    private static final LocalDateTime START_TIME = LocalDateTime.of(
            2017, Month.JANUARY, 31, 10, 40, 0);

    @Test
    public void testAddsPathToSector() {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 0, 5,
                new PreloadedReachabilityClient(Collections.emptyMap()),
                new ForwardTimeTracker(), Collections.emptySet(),
                new SerialWorkAllocator());

        visitor.visit(sector);

        Assert.assertEquals(PATH, sector.getBestPaths().get(task));
    }

    @Test
    public void testAddsPathToContainingSector() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 0, 5,
                new PreloadedReachabilityClient(Collections.emptyMap()),
                new ForwardTimeTracker(), Collections.emptySet(),
                new SerialWorkAllocator());

        visitor.visit(landmark);

        Assert.assertEquals(PATH, sector.getBestPaths().get(task));
    }

    @Test
    public void testAddsPathToLandmark() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 0, 5,
                new PreloadedReachabilityClient(Collections.emptyMap()),
                new ForwardTimeTracker(), Collections.emptySet(),
                new SerialWorkAllocator());

        visitor.visit(landmark);

        Assert.assertEquals(PATH, landmark.getBestPaths().get(task));
    }

    @Test
    public void testAddsPathToTransitStop() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 0, 5,
                new PreloadedReachabilityClient(Collections.emptyMap()),
                new ForwardTimeTracker(), Collections.emptySet(),
                new SerialWorkAllocator());

        visitor.visit(stop);

        Assert.assertEquals(PATH, stop.getBestPaths().get(task));
    }

    @Test
    public void testVisitsReachable() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 1, 5,
                new PreloadedReachabilityClient(ImmutableMap.of(
                        stop, new WalkingCosts(Duration.ofMinutes(3), 3.0))),
                new PreloadedTimeTracker(LocalDateTime.of(
                        2017, Month.JANUARY, 21, 10, 48, 0), true,
                                         Duration.ofMinutes(3)),
                ImmutableSet.of(visitorFactory),
                new SerialWorkAllocator());

        visitor.visit(landmark);
        Assert.assertEquals(1, visitorFactory.getVisitors().size());
        Assert.assertEquals(1, visitorFactory.getVisitors().get(0)
                            .getTransitStopCount());
    }

    @Test
    public void testNoVisitsWhenBeyondOfTime() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 1, 5,
                new PreloadedReachabilityClient(ImmutableMap.of(
                        stop, new WalkingCosts(Duration.ofMinutes(3), 3.0))),
                new PreloadedTimeTracker(LocalDateTime.of(
                        2017, Month.JANUARY, 21, 10, 48, 0), false,
                                         Duration.ofMinutes(3)),
                ImmutableSet.of(visitorFactory),
                new SerialWorkAllocator());

        visitor.visit(landmark);
        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

    @Test
    public void testNoVisitsWhenOutOfDepth() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 5, 5,
                new PreloadedReachabilityClient(ImmutableMap.of(
                        stop, new WalkingCosts(Duration.ofMinutes(3), 3.0))),
                new PreloadedTimeTracker(LocalDateTime.of(
                        2017, Month.JANUARY, 21, 10, 48, 0), true,
                                         Duration.ofMinutes(3)),
                ImmutableSet.of(visitorFactory),
                new SerialWorkAllocator());

        visitor.visit(landmark);
        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

    @Test
    public void testNoVisitsWhenWasWalking() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.WALKING, null, PATH, 1, 5,
                new PreloadedReachabilityClient(ImmutableMap.of(
                        stop, new WalkingCosts(Duration.ofMinutes(3), 3.0))),
                new PreloadedTimeTracker(LocalDateTime.of(
                        2017, Month.JANUARY, 21, 10, 48, 0), true,
                                         Duration.ofMinutes(3)),
                ImmutableSet.of(visitorFactory),
                new SerialWorkAllocator());

        visitor.visit(landmark);
        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

    @Test
    public void testNoVisitsWhenBetterPath() throws Exception {
        final Sector sector = new Sector(new Geodetic2DBounds(
                new Geodetic2DPoint(
                        new Longitude(-122.459696, Longitude.DEGREES),
                        new Latitude(47.734145, Latitude.DEGREES)),
                new Geodetic2DPoint(
                        new Longitude(-122.224433, Longitude.DEGREES),
                        new Latitude(47.48172, Latitude.DEGREES))));
        final Landmark landmark = new Landmark(
                sector, new Geodetic2DPoint(
                        new Longitude(-122.355188, Longitude.DEGREES),
                        new Latitude(47.6256076, Latitude.DEGREES)));
        final TransitStop stop = new TransitStop(
                sector, "1", "Somewhere", new Geodetic2DPoint(
                        new Longitude(-122.325386, Longitude.DEGREES),
                        new Latitude(47.63411, Latitude.DEGREES)));
        final TaskIdentifier task = new TaskIdentifier(
                START_TIME, new Landmark(sector, sector.getCanonicalPoint()),
                "test");
        stop.replacePath(task, PATH);

        final CountingVisitorFactory visitorFactory
                = new CountingVisitorFactory();

        final WalkVisitor visitor = new WalkVisitor(
                task, LocalDateTime.of(2017, Month.JANUARY, 21, 10, 45, 0),
                LocalDateTime.of(2017, Month.JANUARY, 21, 10, 50, 0),
                Mode.NONE, null, PATH, 1, 5,
                new PreloadedReachabilityClient(ImmutableMap.of(
                        stop, new WalkingCosts(Duration.ofMinutes(3), 3.0))),
                new PreloadedTimeTracker(LocalDateTime.of(
                        2017, Month.JANUARY, 21, 10, 48, 0), true,
                                         Duration.ofMinutes(3)),
                ImmutableSet.of(visitorFactory),
                new SerialWorkAllocator());

        visitor.visit(landmark);
        Assert.assertTrue(visitorFactory.getVisitors().isEmpty());
    }

}
