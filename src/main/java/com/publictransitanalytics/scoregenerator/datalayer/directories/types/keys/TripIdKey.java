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

import com.bitvantage.bitvantagecaching.Key;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Key for a specific trip that may be time-qualified.
 *
 * @author Public Transit Analytics
 */
@Value
@AllArgsConstructor
public class TripIdKey implements Key {

    @NonNull
    private final String rawTripId;
    private final String qualifier;

    public TripIdKey(final String rawTripId) {
        this(rawTripId, null);
    }

    @Override
    public String getKeyString() {
        if (qualifier != null) {
            return String.format("%s_%s", rawTripId, qualifier);
        }
        return rawTripId;
    }
}
