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
package com.publictransitanalytics.scoregenerator.testhelpers;

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.Rider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.rider.ScheduleReader;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedRiderBehaviorFactory implements RiderFactory {

    private final PreloadedScheduleReader reader;
    private final PreloadedRider rider;

    @Override
    public ScheduleReader getScheduleReader() {
        return reader;
    }

    @Override
    public Rider getNewRider(TransitStop stop, LocalDateTime initialTime,
                             LocalDateTime cutoffTime, Trip trip) {
        return rider;
    }

}
