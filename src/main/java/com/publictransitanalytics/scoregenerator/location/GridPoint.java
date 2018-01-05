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
package com.publictransitanalytics.scoregenerator.location;

import com.publictransitanalytics.scoregenerator.GeoAngle;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.environment.Segment;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import lombok.EqualsAndHashCode;

/**
 *
 * @author Public Transit Analytics
 */
@EqualsAndHashCode(of = "identifier")
public class GridPoint extends PointLocation {

    private final String name;
    private final String identifier;

    public GridPoint(final GeoPoint location, final Segment segment,
                     final GeoAngle angle) {
        super(location);
        name = location.toString();
        identifier = String.format("gridpoint:%sx%s", segment.toString(),
                                   angle.toString());
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getCommonName() {
        return name;
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public void accept(final Visitor visitor) throws InterruptedException {
        visitor.visit(this);
    }

}
