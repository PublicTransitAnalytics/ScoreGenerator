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
 * A record of a transit vehicle's current location in the context of its path
 * on a trip.
 * 
 * @author Public Transit Analytics
 */
@Value
public class TripStop {

    private final TransitTime time;
    private final String stopId;
    private final TripId tripId;
    private final int sequence;

}
