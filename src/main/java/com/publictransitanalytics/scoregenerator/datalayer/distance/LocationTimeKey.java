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
package com.publictransitanalytics.scoregenerator.datalayer.distance;

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author Public Transit Analytics
 */
public class LocationTimeKey extends RangedKey<LocationTimeKey> {

    private static final int MAX_TIME_SECONDS = 86400;

    @NonNull
    private final String originId;
    @Getter
    private final int timeSeconds;
    @NonNull
    @Getter
    private final String destinationId;
    @Getter
    private final String keyString;

    private LocationTimeKey(final String originId, final int timeSeconds,
                            final String destinationId) {
        if (timeSeconds > MAX_TIME_SECONDS) {
            throw new IllegalArgumentException(String.format(
                    "Distance %d is greater than the max distance.",
                    timeSeconds));
        }
        if (timeSeconds < 0) {
            throw new IllegalArgumentException(String.format(
                    "Distance %d is less than the min distance.",
                    timeSeconds));
        }
        this.originId = originId;
        this.timeSeconds = timeSeconds;
        this.destinationId = destinationId;
        keyString = String.format("%s::%05d::%s", originId, timeSeconds,
                             destinationId);
    }

    public static LocationTimeKey getWriteKey(final String originId,
                                              final int time,
                                              final String destinationId) {
        return new LocationTimeKey(originId, time, destinationId);
    }

    public static LocationTimeKey getMinKey(final String originId,
                                            final int time) {
        return new LocationTimeKey(originId, time, "");
    }

    public static LocationTimeKey getMaxKey(final String originId,
                                            final int time) {
        return new LocationTimeKey(originId, time + 1, "");
    }

    @Override
    public LocationTimeKey getRangeMin() {
        return getMinKey(originId, 0);
    }

    @Override
    public LocationTimeKey getRangeMax() {
        return getMaxKey(originId, MAX_TIME_SECONDS);
    }

    public static class Materializer implements
            KeyMaterializer<LocationTimeKey> {

        final Pattern pattern = Pattern.compile(
                "(.+)::(\\d{5})::(.+)");

        @Override
        public LocationTimeKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = pattern.matcher(keyString);
            if (matcher.matches()) {
                final String originId = matcher.group(1);
                final String timeString = matcher.group(2);
                final String destinationId = matcher.group(3);
                return new LocationTimeKey(
                        originId, Integer.valueOf(timeString), destinationId);
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }
    }

}
