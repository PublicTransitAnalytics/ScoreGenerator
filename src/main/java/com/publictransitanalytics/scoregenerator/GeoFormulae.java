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
package com.publictransitanalytics.scoregenerator;

/**
 *
 * @author Public Transit Analytics
 */
public class GeoFormulae {

    // Adapted from http://www.edwilliams.org/avform.htm#Math
    public static double geoMod(final double y, final double x) {
        final double mod = y - (x * (int) (y / x));
        if (mod < 0) {
            return mod + x;
        } else {
            return mod;
        }
    }

}
