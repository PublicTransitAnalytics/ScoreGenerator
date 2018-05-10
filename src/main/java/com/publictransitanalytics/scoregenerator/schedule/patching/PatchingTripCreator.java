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
package com.publictransitanalytics.scoregenerator.schedule.patching;

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class PatchingTripCreator {

    private final Set<Trip> trips;

    public PatchingTripCreator(
            final List<Patch> patches,
            final TransitNetwork baseNetwork) {
        
        final ImmutableSet.Builder<Trip> builder = ImmutableSet.builder();
        for (final Trip originalTrip : baseNetwork.getTrips()) {
            Trip trip = originalTrip;
            boolean retain = true;
            for (final Patch patch : patches) {
                final Optional<Trip> result = patch.patch(trip);
                if (!result.isPresent()) {
                    retain = false;
                    break;                  
                }
                trip = result.get();
            }
            if (retain) {
                builder.add(trip);
            }
        }
        trips = builder.build();
    }

    public Set<Trip> createTrips() throws InterruptedException {
        return trips;
    }

}
