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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRecord;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A way of tracking reachability.
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public abstract class ScoreCard {

    @Getter
    private final int taskCount;

    public abstract int getReachedCount(final Sector location);

    public abstract boolean hasPath(final Sector location);

    public abstract void scoreTask(
            final TaskIdentifier task,
            final Map<PointLocation, DynamicProgrammingRecord> stateMap);

}