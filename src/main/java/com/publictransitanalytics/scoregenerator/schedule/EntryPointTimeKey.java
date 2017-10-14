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
package com.publictransitanalytics.scoregenerator.schedule;

import com.fasterxml.uuid.UUIDComparator;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Value;

/**
 *
 * @author Public Transit Analytics
 */
@Value
public class EntryPointTimeKey implements Comparable<EntryPointTimeKey> {

    private static final UUID MIN_UUID = new UUID(0, 0);
    private static final UUID MAX_UUID = new UUID(0xFFFFFFFFFFFFFFFFL,
                                                  0xFFFFFFFFFFFFFFFFL);
    
    private final LocalDateTime time;
    private final UUID uuid;

    public EntryPointTimeKey(final LocalDateTime time) {
        this(time, UUID.randomUUID());
    }

    private EntryPointTimeKey(final LocalDateTime time, final UUID uuid) {
        this.time = time;
        this.uuid = uuid;
    }

    @Override
    public int compareTo(final EntryPointTimeKey o) {
        final LocalDateTime otherTime = o.getTime();
        if (time.equals(otherTime)) {
            return UUIDComparator.staticCompare(uuid, o.getUuid());
        }
        return time.compareTo(otherTime);
    }

    public static EntryPointTimeKey getMinimalKey(final LocalDateTime time) {
        return new EntryPointTimeKey(time, MIN_UUID);
    }

    public static EntryPointTimeKey getMaximalKey(final LocalDateTime time) {
        return new EntryPointTimeKey(time, MAX_UUID);
    }
}
