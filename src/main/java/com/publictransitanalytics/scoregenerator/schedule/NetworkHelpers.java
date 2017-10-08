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

import com.google.common.collect.TreeBasedTable;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import java.time.LocalDateTime;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
public final class NetworkHelpers {

    public static TreeBasedTable<TransitStop, LocalDateTime, EntryPoint> makeEntyPointTable(
            final Set<Trip> trips) {
        final TreeBasedTable<TransitStop, LocalDateTime, EntryPoint> entryPoints
                = TreeBasedTable.create(
                        (stop1, stop2) -> stop1.getIdentifier().compareTo(
                                stop2.getIdentifier()),
                        (time1, time2) -> time1.compareTo(time2));
        for (final Trip trip : trips) {
            for (final ScheduledLocation scheduledLocation
                         : trip.getSchedule()) {
                final LocalDateTime time = scheduledLocation.getScheduledTime();
                final TransitStop transitStop
                        = scheduledLocation.getLocation();
                final EntryPoint entryPoint = new EntryPoint(trip, time);
                entryPoints.put(transitStop, time, entryPoint);
            }
        }
        return entryPoints;
    }

    private NetworkHelpers() {
    }
}
