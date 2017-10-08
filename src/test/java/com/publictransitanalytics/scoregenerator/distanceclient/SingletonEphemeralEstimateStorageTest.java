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
import com.publictransitanalytics.scoregenerator.location.Sector;
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
public class SingletonEphemeralEstimateStorageTest {

    private static final Sector SECTOR = new Sector(new Geodetic2DBounds(
            new Geodetic2DPoint(
                    new Longitude(-122.459696, Longitude.DEGREES),
                    new Latitude(47.734145, Latitude.DEGREES)),
            new Geodetic2DPoint(
                    new Longitude(-122.224433, Longitude.DEGREES),
                    new Latitude(47.48172, Latitude.DEGREES))));
    private static final PointLocation ORIGIN = new Landmark(
            SECTOR, new Geodetic2DPoint(
                    new Longitude(-122.325386, Longitude.DEGREES),
                    new Latitude(47.63411, Latitude.DEGREES)));
    private static final PointLocation DESTINATION = new Landmark(
            SECTOR, new Geodetic2DPoint(
                    new Longitude(-122.3198155, Longitude.DEGREES),
                    new Latitude(47.6673156, Latitude.DEGREES)));

    @Test
    public void testGetsInRangeOfOrigin() throws Exception {
        final SingletonEphemeralEstimateStorage storage
                = new SingletonEphemeralEstimateStorage(ORIGIN);
        storage.put(ORIGIN, DESTINATION, 10);
        Assert.assertEquals(Collections.singleton(DESTINATION),
                            storage.getReachable(ORIGIN, 10));
    }

    @Test
    public void testDoesNotGetOutofRangeOfOrigin() throws Exception {
        final SingletonEphemeralEstimateStorage storage
                = new SingletonEphemeralEstimateStorage(ORIGIN);
        storage.put(ORIGIN, DESTINATION, 10);
        Assert.assertTrue(storage.getReachable(ORIGIN, 9).isEmpty());
    }

    @Test
    public void testGetsInRangeOfDestination() throws Exception {
        final SingletonEphemeralEstimateStorage storage
                = new SingletonEphemeralEstimateStorage(ORIGIN);
        storage.put(DESTINATION, ORIGIN, 10);
        Assert.assertEquals(Collections.singleton(ORIGIN),
                            storage.getReachable(DESTINATION, 10));
    }

    @Test
    public void testDoesNotGetOutOfRangeOfDestianion() throws Exception {
 final SingletonEphemeralEstimateStorage storage
                = new SingletonEphemeralEstimateStorage(ORIGIN);
        storage.put(DESTINATION, ORIGIN, 10);
        Assert.assertTrue(storage.getReachable(DESTINATION, 9).isEmpty());
    }

}
