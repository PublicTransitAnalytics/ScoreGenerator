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
package com.publictransitanalytics.scoregenerator.rider;

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class ForwardScheduleReader implements ScheduleReader {

    private final TransitNetwork entryPoints;

    @Override
    public Set<EntryPoint> getEntryPoints(
            final TransitStop position,
            final LocalDateTime currentTime, final LocalDateTime cutoffTime) {
        return entryPoints.getEntryPoints(position, currentTime, cutoffTime);
    }

}
