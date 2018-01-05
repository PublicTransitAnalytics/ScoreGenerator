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
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import com.publictransitanalytics.scoregenerator.environment.Segment;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedEstimateStorage;
import com.publictransitanalytics.scoregenerator.testhelpers.RecordingEstimator;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class CompletePairGeneratorTest {

    private static final GeoLatitude LATITUDE
            = new GeoLatitude("47.66", AngleUnit.DEGREES);
    private static final Segment SEGMENT = new Segment(
            new GeoPoint(
                    new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                    new GeoLatitude("47.661389", AngleUnit.DEGREES)),
            new GeoPoint(
                    new GeoLongitude("-122.43361", AngleUnit.DEGREES),
                    new GeoLatitude("47.661389", AngleUnit.DEGREES)), 0, 0);

    private static final GridPoint GRID_POINT = new GridPoint(
            new GeoPoint(
                    new GeoLongitude("-122.33618", AngleUnit.DEGREES),
                    new GeoLatitude("47.63", AngleUnit.DEGREES)), SEGMENT,
            LATITUDE);
    private static final TransitStop STOP = new TransitStop(
            "stop", "stop", new GeoPoint(
                    new GeoLongitude("-122.33618", AngleUnit.DEGREES),
                    new GeoLatitude("47.620691", AngleUnit.DEGREES)));
    private static final PointLocation CENTER = new Landmark(
            new GeoPoint(
                    new GeoLongitude("-122.34", AngleUnit.DEGREES),
                    new GeoLatitude("47.63", AngleUnit.DEGREES)));

    @Test
    public void testCenterAsOrigin() throws Exception {

        final CompletePairGenerator generator = new CompletePairGenerator(
                Collections.singleton(GRID_POINT), Collections.singleton(STOP),
                Collections.singleton(CENTER));

        final RecordingEstimator estimator = new RecordingEstimator();
        final EstimateStorage storage = new PreloadedEstimateStorage(false, 0);

        generator.storeEstimates(estimator, storage, 1);
        Assert.assertTrue(estimator.getRecord().containsKey(CENTER));
        Assert.assertEquals(ImmutableSet.of(STOP, GRID_POINT),
                            estimator.getRecord().get(CENTER));
    }

    @Test
    public void testStopAsOrigin() throws Exception {

        final CompletePairGenerator generator = new CompletePairGenerator(
                Collections.singleton(GRID_POINT), Collections.singleton(STOP),
                Collections.singleton(CENTER));

        final RecordingEstimator estimator = new RecordingEstimator();
        final EstimateStorage storage = new PreloadedEstimateStorage(false, 0);

        generator.storeEstimates(estimator, storage, 1);
        Assert.assertTrue(estimator.getRecord().containsKey(STOP));
        Assert.assertEquals(Collections.singleton(GRID_POINT),
                            estimator.getRecord().get(STOP));
    }

    @Test
    public void testGridPointAsOrigin() throws Exception {

        final CompletePairGenerator generator = new CompletePairGenerator(
                Collections.singleton(GRID_POINT), Collections.singleton(STOP),
                Collections.singleton(CENTER));

        final RecordingEstimator estimator = new RecordingEstimator();
        final EstimateStorage storage = new PreloadedEstimateStorage(false, 0);

        generator.storeEstimates(estimator, storage, 1);
        Assert.assertFalse(estimator.getRecord().containsKey(GRID_POINT));
    }

}
