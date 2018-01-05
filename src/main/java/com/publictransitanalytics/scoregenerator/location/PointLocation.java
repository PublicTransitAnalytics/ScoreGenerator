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

import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import lombok.Getter;

/**
 * Describes a place, whether a point or region, that a person can reach.
 *
 * @author Public Transit Analytics
 */
public abstract class PointLocation {

    @Getter
    private final GeoPoint location;

    public PointLocation(final GeoPoint location) {
        this.location = location;
    }
    
    public abstract String getIdentifier();

    public abstract String getCommonName();

    public abstract void accept(Visitor visitor) throws InterruptedException;

}
