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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
public interface EstimateStorage {

    public void put(final PointLocation origin,
                    final PointLocation destination,
                    final double distanceMeters) throws InterruptedException;

    public Set<PointLocation> getReachable(final PointLocation origin,
                                               final double distanceMeters)
            throws InterruptedException;

    public boolean isInitialized() throws InterruptedException;

    public double getMaxStored(final PointLocation origin)
            throws InterruptedException;

    public void updateMaxStored(final PointLocation orgin, final double max)
            throws InterruptedException;

    public void close();

}
