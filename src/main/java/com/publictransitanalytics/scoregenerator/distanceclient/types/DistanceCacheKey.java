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
package com.publictransitanalytics.scoregenerator.distanceclient.types;

import com.bitvantage.bitvantagecaching.Key;
import lombok.Value;

/**
 * A key describing the distance between an origin and a destination.
 * 
 * @author Public Transit Analytics 
 */
@Value
public class DistanceCacheKey implements Key {

    String originId;
    String destinationId;

    @Override
    public String getKeyString() {
        return String.format("%s::%s", originId, destinationId);
    }
}
