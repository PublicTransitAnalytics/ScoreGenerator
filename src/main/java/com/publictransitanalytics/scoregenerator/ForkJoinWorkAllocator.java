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
import com.publictransitanalytics.scoregenerator.visitors.VisitAction;
import com.publictransitanalytics.scoregenerator.visitors.Visitation;
import lombok.RequiredArgsConstructor;

/**
 * Allocates work by submitting to a ForkJoin pool. Assumes the method using
 * calling the work method in this class is already in a ForkJoin context.
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class ForkJoinWorkAllocator implements WorkAllocator {

    private final int maxTaskSize;

    @Override
    public void work(final ImmutableList<Visitation> visitations)
            throws InterruptedException {
        final VisitAction action = 
                new VisitAction(visitations, maxTaskSize);
        action.fork();
        action.join();
    }

}
