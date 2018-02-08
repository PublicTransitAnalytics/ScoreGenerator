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

import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.workflow.MovementAssembler;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PathScoreCardFactory implements ScoreCardFactory<PathScoreCard> {

    private final MovementAssembler assembler;
    private final TimeTracker timeTracker;

    @Override
    public PathScoreCard makeScoreCard(
            final int taskCount,
            final SetMultimap<PointLocation, Sector> pointSectorMap) {
        return new PathScoreCard(taskCount, pointSectorMap, assembler,
                                 timeTracker);
    }

}
