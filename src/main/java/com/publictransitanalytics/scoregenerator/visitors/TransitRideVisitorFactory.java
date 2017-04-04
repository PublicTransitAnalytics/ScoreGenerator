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
package com.publictransitanalytics.scoregenerator.visitors;

import com.publictransitanalytics.scoregenerator.Mode;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.rider.RiderBehaviorFactory;

/**
 * Creates a TransitRideVisitor.
 * 
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class TransitRideVisitorFactory implements VisitorFactory {

    private final int maxDepth;
    private final RiderBehaviorFactory riderFactory;
    
    @Override
    public Visitor getVisitor(final LocalDateTime keyTime,
                              final LocalDateTime cutoffTime,
                              final LocalDateTime currentTime, 
                              final Mode lastMode, final TripId lastTrip,
                              final MovementPath currentPath,
                              final int currentDepth,
                              final Set<VisitorFactory> visitorFactories) {
        return new TransitRideVisitor(keyTime, cutoffTime, currentTime,
                currentPath, lastTrip, currentDepth, maxDepth, riderFactory, 
                visitorFactories);

    }

}
