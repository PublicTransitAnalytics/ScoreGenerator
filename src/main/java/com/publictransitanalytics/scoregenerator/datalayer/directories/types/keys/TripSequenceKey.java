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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * The key a trip and its sequence in the trip.
 *
 * @author Public Transit Analytics
 */
public class TripSequenceKey extends RangedKey<TripSequenceKey> {

    @NonNull
    private final TripIdKey tripIdKey;
    private final TransitTime stopTime;
    private final int sequence;

    private final String keyString;

    public TripSequenceKey(final TripIdKey tripIdKey,
                           final TransitTime stopTime, final int sequence) {
        this.tripIdKey = tripIdKey;
        this.stopTime = stopTime;
        this.sequence = sequence;
        keyString = String.format("%s::%s::%010d", tripIdKey.getKeyString(),
                                  stopTime.toString(), sequence);
    }

    @Override
    public TripSequenceKey getRangeMin() {
        return new TripSequenceKey(tripIdKey, TransitTime.MIN_TRANSIT_TIME, 0);
    }

    @Override
    public TripSequenceKey getRangeMax() {
        return new TripSequenceKey(tripIdKey, TransitTime.MAX_TRANSIT_TIME,
                                   Integer.MAX_VALUE);
    }

    @Override
    public String getKeyString() {
        return keyString;
    }

    public static class Materializer
            implements KeyMaterializer<TripSequenceKey> {

        final Pattern pattern = Pattern.compile("(.+)::(.+)::(\\d{10})");

        @Override
        public TripSequenceKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = pattern.matcher(keyString);
            if (matcher.matches()) {
                final String tripIdString = matcher.group(1);
                final String stopTimeString = matcher.group(2);
                final String sequenceString = matcher.group(3);
                return new TripSequenceKey(new TripIdKey(tripIdString),
                                           TransitTime.parse(stopTimeString),
                                           Integer.valueOf(sequenceString));
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }
    }

}
