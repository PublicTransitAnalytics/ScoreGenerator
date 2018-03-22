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

import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;

/**
 * A visitable point of significance.
 *
 * @author Public Transit Analytics.
 */
public class Landmark extends PointLocation implements LogicalCenter {

    private final String name;
    private final String id;

    public Landmark(final GeoPoint location) {
        super(location);
        name = location.toDegreeString();
        id = location.toString();
    }

    @Override
    public void accept(Visitor visitor) throws InterruptedException {
        visitor.visit(this);
    }

    @Override
    public String getIdentifier() {
        return id;
    }

    @Override
    public String getCommonName() {
        return name;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public GeoPoint getPointRepresentation() {
        return getLocation();
    }

}
