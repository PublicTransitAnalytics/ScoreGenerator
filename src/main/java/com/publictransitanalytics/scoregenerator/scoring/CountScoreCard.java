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
import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRecord;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ScoreCard that counts how many tasks reach a Sector.
 *
 * @author Public Transit Analytics
 */
public class CountScoreCard extends ScoreCard {

    private final Multiset<Sector> locations;
    private final SetMultimap<PointLocation, Sector> pointSectorMap;

    public CountScoreCard(
            final int taskCount,
            final SetMultimap<PointLocation, Sector> pointSectorMap) {
        super(taskCount);
        locations = ConcurrentHashMultiset.create();
        this.pointSectorMap = pointSectorMap;
    }

    @Override
    public int getReachedCount(final Sector location) {
        return locations.count(location);
    }

    @Override
    public boolean hasPath(final Sector location) {
        return locations.contains(location);
    }

    @Override
    public void scoreTask(
            final TaskIdentifier task,
            final Map<PointLocation, DynamicProgrammingRecord> stateMap) {
        final Set<PointLocation> reachedLocations = stateMap.keySet();
        final Set<Sector> reachedSectors = reachedLocations.stream().map(
                location -> pointSectorMap.get(location))
                .flatMap(Collection::stream).collect(Collectors.toSet());
        locations.addAll(reachedSectors);
    }

}
