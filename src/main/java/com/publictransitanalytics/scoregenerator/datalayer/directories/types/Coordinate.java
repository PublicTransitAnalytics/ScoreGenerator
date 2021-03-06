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
package com.publictransitanalytics.scoregenerator.datalayer.directories.types;

import lombok.Value;

/**
 * A position on the surface of the earth.
 * 
 * @author Public Transit Analytics
 */
@Value
public class Coordinate {

    String latitude;
    String longitude;

    public Coordinate(final String coordinateString) {
        String coordinateStrings[] = coordinateString.split(",");
        latitude = coordinateStrings[0];
        longitude = coordinateStrings[1];
    }

    public Coordinate(final String latitude, final String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return String.format("%s,%s", latitude, longitude);
    }

}
