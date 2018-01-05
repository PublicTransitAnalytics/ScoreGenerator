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
package com.publictransitanalytics.scoregenerator.environment;

import com.publictransitanalytics.scoregenerator.GeoPoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class Segment {

    @Getter
    private final GeoPoint northPoint;
    @Getter
    private final GeoPoint southPoint;
    @Getter
    private final GeoPoint eastPoint;
    @Getter
    private final GeoPoint westPoint;
    private final String identifier;

    public Segment(final GeoPoint point1, final GeoPoint point2,
                   final long wayId, final int nodeIndex) {
        if (point1.getLatitude().compareTo(point2.getLatitude()) < 0) {
            southPoint = point1;
            northPoint = point2;
        } else {
            northPoint = point1;
            southPoint = point2;
        }

        if (point1.getLongitude().compareTo(point2.getLongitude()) < 0) {
            westPoint = point1;
            eastPoint = point2;
        } else {
            eastPoint = point1;
            westPoint = point2;
        }
        identifier = String.format("segment:%d-%d", wayId, nodeIndex);
    }
    
    @Override
    public String toString() {
        return identifier;
    }

}
