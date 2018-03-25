/*
 * Copyright 2018 Public Transit Analytics.
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

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.StoreBackedRangedKeyStore;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.scoring.ScoreMappingKey;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRecord;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Public Transit Analytics
 */
public class StoringMappingScoreCard extends ScoreCard {

    private final StoreBackedRangedKeyStore<ScoreMappingKey> store;
    private final SetMultimap<PointLocation, Sector> pointSectorMap;

    public StoringMappingScoreCard(
            final int taskCount,
            final StoreBackedRangedKeyStore<ScoreMappingKey> store,
            final SetMultimap<PointLocation, Sector> pointSectorMap) {
        super(taskCount);
        this.store = store;
        this.pointSectorMap = pointSectorMap;
    }

    @Override
    public int getReachedCount(final Sector location)
            throws InterruptedException {
        try {
            return store.getValuesInRange(
                    ScoreMappingKey.getMinKey(location.getIdentifier()),
                    ScoreMappingKey.getMaxKey(location.getIdentifier())).size();
        } catch (BitvantageStoreException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public boolean hasPath(final Sector location) throws InterruptedException {
        return getReachedCount(location) > 0;
    }

    @Override
    public void scoreTask(
            final TaskIdentifier task,
            final Map<PointLocation, DynamicProgrammingRecord> stateMap)
            throws InterruptedException {
        final Set<PointLocation> reachedLocations = stateMap.keySet();
        final Set<Sector> reachedSectors = reachedLocations.stream().map(
                location -> pointSectorMap.get(location))
                .flatMap(Collection::stream).collect(Collectors.toSet());
        final LogicalTask logicalTask = new LogicalTask(
                task.getTime(), task.getCenter().getLogicalCenter());

        final ImmutableSet.Builder<ScoreMappingKey> keysBuilder
                = ImmutableSet.builder();
        for (final Sector sector : reachedSectors) {
            keysBuilder.add(ScoreMappingKey.getWriteKey(
                    sector.getIdentifier(),
                    logicalTask.getTime().toString(),
                    logicalTask.getCenter().getIdentifier()));
        }

        store.putAll(keysBuilder.build());
    }

}
