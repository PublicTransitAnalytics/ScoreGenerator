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
package com.publictransitanalytics.scoregenerator.datalayer.scoring;

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ScoreMappingKey extends RangedKey<ScoreMappingKey> {

    private final String sectorId;
    private final String date;
    private final String centerId;

    public static ScoreMappingKey getWriteKey(final String originId,
                                              final String date,
                                              final String destinationId) {
        return new ScoreMappingKey(originId, date, destinationId);
    }

    public static ScoreMappingKey getMinKey(final String originId) {
        return new ScoreMappingKey(originId, LocalDateTime.of(
                                   0, Month.JANUARY, 1, 0, 0).toString(), "");
    }

    public static ScoreMappingKey getMaxKey(final String originId) {
        return new ScoreMappingKey(originId, LocalDateTime.of(
                                   9999, Month.DECEMBER, 31, 23, 59).toString(),
                                   "");
    }

    @Override
    public ScoreMappingKey getRangeMin() {
        return getMinKey(sectorId);
    }

    @Override
    public ScoreMappingKey getRangeMax() {
        return getMaxKey(sectorId);
    }

    @Override
    public String getKeyString() {
        return String.format("%s::%s::%s", sectorId, date, centerId);
    }

    public static class Materializer implements
            KeyMaterializer<ScoreMappingKey> {

        final Pattern pattern = Pattern.compile(
                "(.+)::(.+)::(.+)");

        @Override
        public ScoreMappingKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = pattern.matcher(keyString);
            if (matcher.matches()) {
                final String sectorId = matcher.group(1);
                final String date = matcher.group(2);
                final String centerId = matcher.group(3);
                return new ScoreMappingKey(sectorId, date, centerId);
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }
    }

}
