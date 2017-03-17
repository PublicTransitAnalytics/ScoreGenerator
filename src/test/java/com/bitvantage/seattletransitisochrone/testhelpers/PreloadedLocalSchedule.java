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
package com.bitvantage.seattletransitisochrone.testhelpers;

import com.publictransitanalytics.scoregenerator.schedule.LocalSchedule;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripArrival;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author Public Transit Analytics
 */
public class PreloadedLocalSchedule implements LocalSchedule {

    private final NavigableMap<LocalDateTime, Trip> map;

    public PreloadedLocalSchedule(
            final Map<LocalDateTime, Trip> stopTimes) {
        map = new TreeMap<>(stopTimes);
    }

    @Override
    public Set<TripArrival> getArrivalsInRange(final LocalDateTime startTime,
                                            final LocalDateTime endTime) {
        return map.subMap(startTime, true, endTime, true).entrySet().stream()
                .map(entry -> new TripArrival(entry.getValue(), entry.getKey()))
                .collect(Collectors.toSet());
    }

}
