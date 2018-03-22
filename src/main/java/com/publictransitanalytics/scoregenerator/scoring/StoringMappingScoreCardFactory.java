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

import com.bitvantage.bitvantagecaching.RangedStore;
import com.google.common.collect.SetMultimap;
import com.google.common.io.Files;
import com.publictransitanalytics.scoregenerator.StoreFactory;
import com.publictransitanalytics.scoregenerator.datalayer.scoring.ScoreMappingKey;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class StoringMappingScoreCardFactory implements
        ScoreCardFactory<StoringMappingScoreCard> {

    private final StoreFactory factory;

    @Override
    public StoringMappingScoreCard makeScoreCard(
            final int taskCount,
            final SetMultimap<PointLocation, Sector> pointSectorMap) {
        final Path path = Files.createTempDir().toPath();
        final RangedStore<ScoreMappingKey, String> store
                = factory.<ScoreMappingKey, String>getRangedStore(
                        path, new ScoreMappingKey.Materializer(),
                        String.class);
        return new StoringMappingScoreCard(taskCount, store, pointSectorMap);
    }

}
