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

import com.publictransitanalytics.scoregenerator.Mode;
import com.publictransitanalytics.scoregenerator.TaskIdentifier;
import com.publictransitanalytics.scoregenerator.schedule.TripId;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import com.publictransitanalytics.scoregenerator.visitors.VisitorFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Public Transit Analytics
 */
public class CountingVisitorFactory implements VisitorFactory {

    private final List<CountingVisitor> visitors;

    public CountingVisitorFactory() {
        visitors = new ArrayList<>();
    }

    public List<CountingVisitor> getVisitors() {
        return visitors;
    }

    @Override
    public Visitor getVisitor(
            final TaskIdentifier task, final LocalDateTime cutoffTime,
            final LocalDateTime currentTime, final Mode lastMode,
            final TripId lastTrip, final MovementPath currentPath, 
            final int currentDepth, 
            final Set<VisitorFactory> visitorFactories) {
        final CountingVisitor visitor = new CountingVisitor();
        visitors.add(visitor);
        return visitor;
    }

}
