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
package com.publictransitanalytics.scoregenerator.location;

import lombok.Getter;
import org.opensextant.geodesy.Geodetic2DPoint;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;

/**
 * A location where public transit vehicles stop.
 *
 * @author Public Transit Analytics
 */
public class TransitStop extends PointLocation {

    @Getter
    private final String stopId;
    @Getter
    private final String stopName;

    public TransitStop(final Sector sector, final String stopId,
                       final String stopName, final Geodetic2DPoint location) {
        super(sector, location);
        this.stopId = stopId;
        this.stopName = stopName;
    }

    @Override
    public void accept(Visitor visitor) throws InterruptedException {
        visitor.visit(this);
    }

    @Override
    public String getIdentifier() {
        return stopId;
    }

    @Override
    public String getCommonName() {
        return stopName;
    }
    
    @Override
    public String toString() {
        return stopName;
    }

}
