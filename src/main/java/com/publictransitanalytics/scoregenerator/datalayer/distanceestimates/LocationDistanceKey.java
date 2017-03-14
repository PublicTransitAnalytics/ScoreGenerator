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
package com.publictransitanalytics.scoregenerator.datalayer.distanceestimates;

import com.bitvantage.bitvantagecaching.RangedKey;
import java.util.UUID;
import lombok.NonNull;
import lombok.Value;

/**
 * A key containing a base location and a distance. Uses a UUID to avoid collisions, and thus is
 * only useful for ranged queries. Maps to corresponding values.
 * 
 * @author Public Transit Analytics
 */
@Value
public class LocationDistanceKey implements RangedKey<LocationDistanceKey> {

    // Max distance we allow, based on the antipodal distance of earth.
    private static final double MAX_DISTANCE_METERS = 20000000;

    @NonNull
    private final String locationId;
    private final double distanceMeters;
    @NonNull
    private final UUID uuid;

    public static LocationDistanceKey getWriteKey(final String locationId, final double distance) {
        return new LocationDistanceKey(locationId, distance, UUID.randomUUID());
    }

    public static LocationDistanceKey getMinKey(final String locationId, final double distance) {
        return new LocationDistanceKey(locationId, distance, new UUID(0, 0));
    }

    public static LocationDistanceKey getMaxKey(final String locationId, final double distance) {
        return new LocationDistanceKey(locationId, distance,
                                       new UUID((long) - 1, (long) - 1));
    }

    private LocationDistanceKey(final String locationId, final double distanceMeters,
                                final UUID uuid) {
        if (distanceMeters > MAX_DISTANCE_METERS) {
            throw new IllegalArgumentException(String.format(
                "Distance %f is greater than the max distance.", distanceMeters));
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException(String.format(
                "Distance %f is less than the min distance.", distanceMeters));
        }
        this.locationId = locationId;
        this.distanceMeters = distanceMeters;
        this.uuid = uuid;
    }

    @Override
    public LocationDistanceKey getRangeMin() {
        return getMinKey(locationId, 0);
    }

    @Override
    public LocationDistanceKey getRangeMax() {
        return getMaxKey(locationId, MAX_DISTANCE_METERS);
    }

    @Override
    public String getKeyString() {
        return String.format("%s::%015.6f::%s", locationId, distanceMeters, uuid.toString());
    }


}
