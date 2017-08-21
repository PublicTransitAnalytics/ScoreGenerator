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
package com.publictransitanalytics.scoregenerator.scoring;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;

/**
 * ScoreCard that counts how many tasks reach a Sector.
 * 
 * @author Public Transit Analytics
 */
public class CountScoreCard implements ScoreCard {
    
    final Multiset<VisitableLocation> locations;
    
     public CountScoreCard() {
        locations = ConcurrentHashMultiset.create();
    }


    @Override
    public int getReachedCount(final VisitableLocation location) {
        return locations.count(location);
    }

    @Override
    public boolean hasPath(final VisitableLocation location) {
        return locations.contains(location);
    }

    @Override
    public void putPath(final VisitableLocation location,
                        final TaskIdentifier task,
                        final MovementPath path) {
        locations.add(location);
    }
    
}
