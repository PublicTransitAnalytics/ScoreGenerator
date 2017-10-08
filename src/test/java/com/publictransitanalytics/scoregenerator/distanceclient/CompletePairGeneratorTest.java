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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedEstimateStorage;
import com.publictransitanalytics.scoregenerator.testhelpers.RecordingEstimator;
import edu.emory.mathcs.backport.java.util.Collections;
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
public class CompletePairGeneratorTest {

    @Test
    public void testCenterAsOrigin() throws Exception {
        final Sector sector = new Sector(
                new Geodetic2DBounds(
                        new Geodetic2DPoint(
                                new Longitude(-122.459696, Longitude.DEGREES),
                                new Latitude(47.734145, Latitude.DEGREES)),
                        new Geodetic2DPoint(
                                new Longitude(-122.224433, Longitude.DEGREES),
                                new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "stop", "stop",
                new Geodetic2DPoint(new Longitude(-122.3361768,
                                                  Longitude.DEGREES),
                                    new Latitude(47.6206914,
                                                 Latitude.DEGREES)));
        final PointLocation center = new Landmark(
                sector, sector.getCanonicalPoint());

        final CompletePairGenerator generator = new CompletePairGenerator(
                Collections.singleton(sector), Collections.singleton(stop),
                Collections.singleton(center));

        final RecordingEstimator estimator = new RecordingEstimator();
        final EstimateStorage storage = new PreloadedEstimateStorage(false, 0);

        generator.storeEstimates(estimator, storage, 1);
        Assert.assertTrue(estimator.getRecord().containsKey(center));
        Assert.assertEquals(ImmutableSet.of(stop, sector),
                            estimator.getRecord().get(center));
    }

    @Test
    public void testStopAsOrigin() throws Exception {
        final Sector sector = new Sector(
                new Geodetic2DBounds(
                        new Geodetic2DPoint(
                                new Longitude(-122.459696, Longitude.DEGREES),
                                new Latitude(47.734145, Latitude.DEGREES)),
                        new Geodetic2DPoint(
                                new Longitude(-122.224433, Longitude.DEGREES),
                                new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "stop", "stop",
                new Geodetic2DPoint(new Longitude(-122.3361768,
                                                  Longitude.DEGREES),
                                    new Latitude(47.6206914,
                                                 Latitude.DEGREES)));
        final PointLocation center = new Landmark(
                sector, sector.getCanonicalPoint());

        final CompletePairGenerator generator = new CompletePairGenerator(
                Collections.singleton(sector), Collections.singleton(stop),
                Collections.singleton(center));

        final RecordingEstimator estimator = new RecordingEstimator();
        final EstimateStorage storage = new PreloadedEstimateStorage(false, 0);

        generator.storeEstimates(estimator, storage, 1);
        Assert.assertTrue(estimator.getRecord().containsKey(stop));
        Assert.assertEquals(Collections.singleton(sector),
                            estimator.getRecord().get(stop));
    }

    @Test
    public void testSectorAsOrigin() throws Exception {
        final Sector sector = new Sector(
                new Geodetic2DBounds(
                        new Geodetic2DPoint(
                                new Longitude(-122.459696, Longitude.DEGREES),
                                new Latitude(47.734145, Latitude.DEGREES)),
                        new Geodetic2DPoint(
                                new Longitude(-122.224433, Longitude.DEGREES),
                                new Latitude(47.48172, Latitude.DEGREES))));
        final TransitStop stop = new TransitStop(
                sector, "stop", "stop",
                new Geodetic2DPoint(new Longitude(-122.3361768,
                                                  Longitude.DEGREES),
                                    new Latitude(47.6206914,
                                                 Latitude.DEGREES)));
        final PointLocation center = new Landmark(
                sector, sector.getCanonicalPoint());

        final CompletePairGenerator generator = new CompletePairGenerator(
                Collections.singleton(sector), Collections.singleton(stop),
                Collections.singleton(center));

        final RecordingEstimator estimator = new RecordingEstimator();
        final EstimateStorage storage = new PreloadedEstimateStorage(false, 0);

        generator.storeEstimates(estimator, storage, 1);
        Assert.assertFalse(estimator.getRecord().containsKey(sector));
    }

}
