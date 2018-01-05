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

import com.publictransitanalytics.scoregenerator.schedule.patching.PatchingTripCreator;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.schedule.patching.Patch;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedTransitNetwork;
import edu.emory.mathcs.backport.java.util.Collections;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class PatchingTripCreatorTest {

    @Test
    public void testRemovesEmpties() throws Exception {
        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptySet());
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(trip70_1));
        final Patch patch = trip -> Optional.empty();

        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.singletonList(patch), baseNetwork);
        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertTrue(trips.isEmpty());
    }

    @Test
    public void testRetainsPresents() throws Exception {
        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptySet());
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(trip70_1));
        final Patch patch = trip -> Optional.of(trip);

        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.singletonList(patch), baseNetwork);
        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(1, trips.size());
        Assert.assertEquals(trip70_1, trips.iterator().next());
    }

    @Test
    public void testAcceptsChanges() throws Exception {
        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptySet());
        final Trip trip70_1Modified
                = new Trip(new TripId("70_1M", LocalDate.MIN),
                           "70", "70", Collections.emptySet());
        final TransitNetwork baseNetwork = new PreloadedTransitNetwork(
                ImmutableSet.of(trip70_1));
        final Patch patch = trip -> Optional.of(trip70_1Modified);

        final PatchingTripCreator tripCreator = new PatchingTripCreator(
                Collections.singletonList(patch), baseNetwork);
        final Set<Trip> trips = tripCreator.createTrips();
        Assert.assertEquals(1, trips.size());
        Assert.assertEquals(trip70_1Modified, trips.iterator().next());
    }

}
