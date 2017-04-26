/*
 * Copyright 2016 matt.
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
package com.publictransitanalytics.scoregenerator.datalayer.directories;

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripStop;
import java.util.List;
import java.util.Set;

/**
 * Directory that describes where transit vehicles are located. Allows
 * navigation both at the level of a stop and of a trip.
 */
public interface StopTimesDirectory {

    /**
     * Gets the stops on a given transit trip between the two specified times.
     *
     * @param tripId The id of the trip.
     * @param startTime The beginning, inclusive, of the time range to search.
     * @param endTime The end, inclusive, of the time range to search.
     * @return The TripStops making up the portion of the trip in the interval.
     */
    List<TripStop> getStopsOnTripInRange(
            final TripId tripId, final TransitTime startTime,
            final TransitTime endTime) throws InterruptedException;

    /**
     * Gets the stops on a given transit trip after a given time.
     *
     * @param tripId The id of the trip.
     * @param startTime The beginning, inclusive, of the time range to search.
     * @return The TripStops making up the portion of the trip in the interval.
     */
    List<TripStop> getSubsequentStopsOnTrip(
            final TripId tripId, final TransitTime startTime)
            throws InterruptedException;

    /**
     * The the transit trips passing through a stop between the two specified
     * times.
     *
     * @param stopId The stop id at which to find trips.
     * @param startTime The beginning, inclusive, of the time range to search.
     * @param endTime The end, inclusive, of the time range to search.
     * @return The TripStops indicating of the trips passing through.
     */
    List<TripStop> getStopsAtStopInRange(
            final String stopId, final TransitTime startTime,
            final TransitTime endTime) throws InterruptedException;

    /**
     * Get all the qualified trip ids. This is the only directory that deals
     * with qualified trip ids, and should not be confused with the TripDetails,
     * which are unqualified.
     *
     * @return A set of a all tripIds.
     */
    Set<TripId> getTripIds() throws InterruptedException;

}
