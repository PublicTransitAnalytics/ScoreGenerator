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
package com.publictransitanalytics.scoregenerator;

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.visitors.VisitAction;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;
import com.publictransitanalytics.scoregenerator.visitors.VisitorFactory;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import lombok.RequiredArgsConstructor;

/**
 * Action for forking off visitors.
 * 
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class ReachedAction extends RecursiveAction {

    private final LocalDateTime startTime;
    private final LocalDateTime cutoffTime;
    private final PointLocation startLocation;
    private final Set<VisitorFactory> visitorFactories;
    private final MovementPath basePath;

    @Override
    protected void compute() {
        final ImmutableList.Builder<VisitAction> visitActionsBuilder
                = ImmutableList.builder();
        for (final VisitorFactory visitorFactory : visitorFactories) {
            final Visitor visitor = visitorFactory.getVisitor(
                    startTime, cutoffTime, startTime, Mode.NONE, null,
                    basePath, 0, visitorFactories);
            final VisitAction visitAction
                    = new VisitAction(startLocation, visitor);
            visitActionsBuilder.add(visitAction);
            visitAction.fork();
        }
        for (final VisitAction visitAction : visitActionsBuilder.build()) {
            visitAction.join();
        }
    }

}
