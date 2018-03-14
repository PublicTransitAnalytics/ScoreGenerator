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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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
 * ScoreCard that maps Sectors to the Centers that allowed them to be reached.
 * This allows a count of the number of times a Sector was reached.
 *
 * @author Public Transit Analytics
 */
public class MappingScoreCard extends ScoreCard {

    private final Multimap<Sector, LogicalTask> locations;
    private final SetMultimap<PointLocation, Sector> pointSectorMap;

    public MappingScoreCard(
            final int taskCount,
            final SetMultimap<PointLocation, Sector> pointSectorMap) {
        super(taskCount);
        locations = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        this.pointSectorMap = pointSectorMap;
    }

    @Override
    public int getReachedCount(final Sector location) {
        return locations.get(location).size();
    }

    @Override
    public boolean hasPath(final Sector location) {
        return locations.containsKey(location);
    }

    @Override
    public void scoreTask(
            final TaskIdentifier task,
            final Map<PointLocation, DynamicProgrammingRecord> stateMap) {
        final Set<PointLocation> reachedLocations = stateMap.keySet();
        final Set<Sector> reachedSectors = reachedLocations.stream().map(
                location -> pointSectorMap.get(location))
                .flatMap(Collection::stream).collect(Collectors.toSet());
        final Set<LogicalTask> logicalTasks = task.getCenter()
                .getLogicalCenters().stream()
                .map(logicalCenter -> new LogicalTask(task.getTime(),
                                                      logicalCenter))
                .collect(Collectors.toSet());

        for (final Sector sector : reachedSectors) {
            for (final LogicalTask logicalTask : logicalTasks) {
                locations.put(sector, logicalTask);
            }
        }
    }

}
