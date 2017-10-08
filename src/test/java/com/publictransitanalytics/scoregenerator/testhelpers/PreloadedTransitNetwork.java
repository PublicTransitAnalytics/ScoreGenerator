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
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import edu.emory.mathcs.backport.java.util.Collections;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedTransitNetwork implements TransitNetwork {

    @Getter
    private final Set<Trip> trips;

    @Override
    public Set<EntryPoint> getEntryPoints(
            final TransitStop stop, final LocalDateTime startTime,
            final LocalDateTime endTime) {
        return Collections.emptySet();
    }

    @Override
    public Duration getInServiceTime() {
        return Duration.ZERO;
    }

}
