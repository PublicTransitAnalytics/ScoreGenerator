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
package com.publictransitanalytics.scoregenerator.workflow;

import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceEstimator;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceFilter;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.geography.EndpointDeterminer;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Value;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import java.time.Duration;
import java.util.NavigableSet;

/**
 *
 * @author Public Transit Analytics
 */
@Value
public class Calculation<S extends ScoreCard> {

    private final Set<TaskGroupIdentifier> taskGroups;
    private final NavigableSet<LocalDateTime> times;
    private final S scoreCard;
    private final TimeTracker timeTracker;
    private final MovementAssembler movementAssembler;
    private final Set<ModeType> allowedModes;
    private final TransitNetwork transitNetwork;
    private final boolean backward;
    private final Duration longestDuration;
    private final double walkingDistanceMetersPerSecond;
    private final EndpointDeterminer endpointDeterminer;
    private final DistanceFilter distanceFilter;
    private final Set<Sector> sectors;
    private final Set<TransitStop> stops;
    private final Set<PointLocation> centers;
    private final DistanceEstimator distanceEstimator;
    private final ReachabilityClient reachabilityClient;
    private final RiderFactory riderFactory;
    
    public int getTaskCount() {
        return taskGroups.size() * times.size();
    }
    
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
    
    @Override
    public boolean equals(final Object other) {
        return other == this;
    }

}