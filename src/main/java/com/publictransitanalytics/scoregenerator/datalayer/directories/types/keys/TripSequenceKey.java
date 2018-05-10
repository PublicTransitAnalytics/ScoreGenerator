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
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.TripId;
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
    private final TripId tripId;
    private final int sequence;

    private final String keyString;

    public TripSequenceKey(final TripId tripId, final int sequence) {
        this.tripId = tripId;
        this.sequence = sequence;
        keyString = String.format("%s_%s::%010d", tripId.getRawTripId(),
                                  tripId.getQualifier(), sequence);
    }

    @Override
    public TripSequenceKey getRangeMin() {
        return new TripSequenceKey(tripId, 0);
    }

    @Override
    public TripSequenceKey getRangeMax() {
        return new TripSequenceKey(tripId, Integer.MAX_VALUE);
    }

    @Override
    public String getKeyString() {
        return keyString;
    }

    public static class Materializer
            implements KeyMaterializer<TripSequenceKey> {

        final Pattern pattern = Pattern.compile("(.+)_(.+)::(\\d{10})");

        @Override
        public TripSequenceKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = pattern.matcher(keyString);
            if (matcher.matches()) {
                final String rawTripIdString = matcher.group(1);
                final String tripIdQualifierString = matcher.group(2);
                final String sequenceString = matcher.group(3);
                return new TripSequenceKey(new TripId(rawTripIdString,
                                                      tripIdQualifierString),
                                           Integer.valueOf(sequenceString));
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }
    }

}
