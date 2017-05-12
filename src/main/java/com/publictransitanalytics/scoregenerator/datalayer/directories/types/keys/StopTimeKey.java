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
package com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys;

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.Value;

/**
 * A key that corresponds to stop and time.
 *
 * @author Public Transit Analytics
 */
@Value
public class StopTimeKey extends RangedKey<StopTimeKey> {

    @NonNull
    private final String stopId;
    @NonNull
    private final TransitTime stopTime;
    @NonNull
    private final UUID uuid;

    public static StopTimeKey getWriteKey(final String stopId,
                                          final TransitTime stopTime) {
        return new StopTimeKey(stopId, stopTime, UUID.randomUUID());

    }

    public static StopTimeKey getMinKey(final String stopId,
                                        final TransitTime stopTime) {
        return new StopTimeKey(stopId, stopTime, new UUID(0, 0));
    }

    public static StopTimeKey getMaxKey(final String stopId,
                                        final TransitTime stopTime) {
        return new StopTimeKey(stopId, stopTime, new UUID(-1, -1));
    }

    private StopTimeKey(final String stopId, final TransitTime stopTime,
                        final UUID uuid) {
        this.stopId = stopId;
        this.stopTime = stopTime;
        this.uuid = uuid;
    }

    @Override
    public String getKeyString() {
        return String.format("%s::%s::%s", stopId, stopTime.toString(),
                             uuid.toString());
    }

    @Override
    public StopTimeKey getRangeMin() {
        return getMinKey(stopId, TransitTime.MIN_TRANSIT_TIME);
    }

    @Override
    public StopTimeKey getRangeMax() {
        return getMaxKey(stopId, TransitTime.MAX_TRANSIT_TIME);
    }

    public static class Materializer implements KeyMaterializer<StopTimeKey> {

        final Pattern pattern = Pattern.compile("(.+)::(.+)::(.+)");

        @Override
        public StopTimeKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = pattern.matcher(keyString);
            if (matcher.matches()) {
                final String stopId = matcher.group(1);
                final String transitTimeString = matcher.group(2);
                final String uuidString = matcher.group(3);
                return new StopTimeKey(
                        stopId, TransitTime.parse(transitTimeString),
                UUID.fromString(uuidString));
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }
    }
}
