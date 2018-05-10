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

import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class RouteDeletionTest {

    @Test
    public void testTurnsToEmptyOptional() {
        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptyList());
        final RouteDeletion deletion = new RouteDeletion("70");
        Assert.assertEquals(Optional.<Trip>empty(), deletion.patch(trip70_1));
    }

    @Test
    public void testRetains() {
        final Trip trip70_1 = new Trip(new TripId("70_1", LocalDate.MIN),
                                       "70", "70", Collections.emptyList());
        final RouteDeletion deletion = new RouteDeletion("71");
        Assert.assertEquals(Optional.of(trip70_1), deletion.patch(trip70_1));
    }

}
