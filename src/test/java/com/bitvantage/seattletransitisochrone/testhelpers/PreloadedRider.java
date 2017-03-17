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

import com.publictransitanalytics.scoregenerator.rider.Rider;
import com.publictransitanalytics.scoregenerator.rider.RiderStatus;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedRider implements Rider {

    private final Set<RiderStatus> entryPoints;
    private final List<RiderStatus> riderStatuses;
    private final Movement movement;
    
    private int calls;

    @Override
    public Set<RiderStatus> getEntryPoints(final LocalDateTime currentTime) {
        return entryPoints;
    }

    @Override
    public void takeTrip(final Trip trip, final LocalDateTime entryTime) {
    }

    @Override
    public boolean canContinueTrip() {
        return calls < riderStatuses.size();
    }

    @Override
    public RiderStatus continueTrip() {
        final RiderStatus status = riderStatuses.get(calls);
        calls++;
        return status;
    }

    @Override
    public Movement getRideRecord() {
        return movement;
    }

}
