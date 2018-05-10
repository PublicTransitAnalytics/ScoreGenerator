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
package com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys;

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TransitTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 *
 * @author Public Transit Analytics
 */
@Value
public class TimeKey extends RangedKey<TimeKey> {

    private final TransitTime time;
    private final UUID uuid;

    @Override
    public String getKeyString() {
        return String.format("%s::%s", time.toString(), uuid.toString());
    }

    @Override
    public TimeKey getRangeMin() {
        return getMinKey(time);
    }

    @Override
    public TimeKey getRangeMax() {
        return getMaxKey(time);
    }

    public static TimeKey getWriteKey(final TransitTime time) {
        return new TimeKey(time, UUID.randomUUID());
    }

    public static TimeKey getMinKey(final TransitTime time) {
        return new TimeKey(time, new UUID(0, 0));
    }

    public static TimeKey getMaxKey(final TransitTime time) {
        return new TimeKey(time, new UUID(-1, -1));
    }

    public static class Materializer implements KeyMaterializer<TimeKey> {

        private static final Pattern PATTERN
                = Pattern.compile("(.+)::(.+)");

        @Override
        public TimeKey materialize(final String keyString) throws
                BitvantageStoreException {
            final Matcher matcher = PATTERN.matcher(keyString);
            if (matcher.matches()) {
                final TransitTime time = TransitTime.parse(matcher.group(1));
                final UUID uuid = UUID.fromString(matcher.group(2));
                return new TimeKey(time, uuid);
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }

    }

}
