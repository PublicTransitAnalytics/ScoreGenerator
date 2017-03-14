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

import com.publictransitanalytics.scoregenerator.schedule.Trip;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Models the capabilities of a transit rider that consults a schedule and takes
 * trips.
 * 
 * @author Public Transit Analytics
 */
public interface Rider {

    Collection<Trip> getTrips(LocalDateTime currentTime);

    public void takeTrip(Trip trip);

    public boolean canContinueTrip();

    public RiderStatus continueTrip();

    public Movement getRideRecord();

}
