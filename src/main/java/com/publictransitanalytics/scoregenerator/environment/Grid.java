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

import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
public interface Grid {

    boolean coversPoint(final GeoPoint location);

    Set<GridPoint> getGridPoints();
    
    Set<GridPoint> getGridPoints(final Sector sector);

    Set<Sector> getSectors(final PointLocation point);
    
    GeoBounds getBounds();
    
    Set<Sector> getAllSectors();
    
    Set<Sector> getReachableSectors();
    
}
