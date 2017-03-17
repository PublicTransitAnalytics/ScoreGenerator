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
package com.publictransitanalytics.scoregenerator.schedule;

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.LocalDateTime;

/**
 * Models the path of a transit vehicle: the stops it takes and the times at 
 * which it makes them.
 * @author Public Transit Analytics
 */
public interface Trip {

    /**
     * Get the next stop on the trip, or null if it does not exist.
     * @param stop The current stop.
     * @param time The current time.
     * @return the next stop.
     */
    ScheduledLocation getNextScheduledLocation(
            TransitStop stop, LocalDateTime time);

    /**
     * Get the previous stop on the trip, or null if it does not exist.
     * @param stop The current stop.
     * @param time The current time.
     * @return the previous  stop.
     */
    ScheduledLocation getPreviousScheduledLocation(
            TransitStop stop, LocalDateTime time);
    
    TripId getTripId();
    
    String getRouteNumber();
    
    String getRouteName();
    
}
