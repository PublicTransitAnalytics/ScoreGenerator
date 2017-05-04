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

import com.google.common.collect.ImmutableList;
import java.util.concurrent.RecursiveAction;
import lombok.RequiredArgsConstructor;

/**
 * Action for a visitor visiting a location.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class VisitAction extends RecursiveAction {

    private final ImmutableList<Visitation> visitations;
    private final int maxTaskSize;

    @Override
    protected void compute() {
        int numVisitations = visitations.size();

        if (numVisitations < maxTaskSize) {
            for (final Visitation visitation : visitations) {
                try {
                    visitation.getLocation().accept(visitation.getVisitor());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            final int half = numVisitations / 2;
            final VisitAction subTasks1 = new VisitAction(
                    visitations.subList(0, half), maxTaskSize);
            subTasks1.fork();
            final VisitAction subTasks2 = new VisitAction(
                    visitations.subList(half, numVisitations), maxTaskSize);
            subTasks2.fork();
            subTasks1.join();
            subTasks2.join();
        }
    }

}
