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

import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import org.opensextant.geodesy.Geodetic2DPoint;

/**
 * A visitable point of significance.
 * 
 * @author Public Transit Analytics.
 */
public class Landmark extends PointLocation {

    public Landmark(final Sector containingSector, 
                    final Geodetic2DPoint location) {
        super(containingSector, location);
    }

    @Override
    public void accept(Visitor visitor) throws InterruptedException {
        visitor.visit(this);
    }

    @Override
    public String getIdentifier() {
        return getLocation().toString();
    }

    @Override
    public String getCommonName() {
        return getLocation().toString();
    }

}
