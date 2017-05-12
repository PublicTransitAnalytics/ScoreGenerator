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

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * A key containing a base location and a distance. Uses a UUID to avoid
 * collisions, and thus is only useful for ranged queries. Maps to corresponding
 * values.
 *
 * @author Public Transit Analytics
 */
public class LocationDistanceKey extends RangedKey<LocationDistanceKey> {

    // Max distance we allow, based on the antipodal distance of earth.
    private static final double MAX_DISTANCE_METERS = 20000000;

    private static final UUID MIN_UUID = new UUID(0, 0);
    private static final UUID MAX_UUID = new UUID((long) - 1, (long) - 1);

    @NonNull
    private final String locationId;
    private final double distanceMeters;
    @NonNull
    private final UUID uuid;

    private final String keyString;

    private LocationDistanceKey(final String locationId,
                                final double distanceMeters,
                                final UUID uuid) {
        if (distanceMeters > MAX_DISTANCE_METERS) {
            throw new IllegalArgumentException(String.format(
                    "Distance %f is greater than the max distance.",
                    distanceMeters));
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException(String.format(
                    "Distance %f is less than the min distance.", distanceMeters));
        }
        this.locationId = locationId;
        this.distanceMeters = distanceMeters;
        this.uuid = uuid;
        keyString = String.format("%s::%015.6f::%s", locationId, distanceMeters,
                                  uuid.toString());
    }

    public static LocationDistanceKey getWriteKey(final String locationId,
                                                  final double distance) {
        return new LocationDistanceKey(locationId, distance, UUID.randomUUID());
    }

    public static LocationDistanceKey getMinKey(final String locationId,
                                                final double distance) {
        return new LocationDistanceKey(locationId, distance, MIN_UUID);
    }

    public static LocationDistanceKey getMaxKey(final String locationId,
                                                final double distance) {
        return new LocationDistanceKey(locationId, distance, MAX_UUID);
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
        return keyString;
    }

    public static class Materializer implements
            KeyMaterializer<LocationDistanceKey> {

        final Pattern pattern = Pattern.compile(
                "(.+)::(\\d{8}\\.\\d{6})::(.+)");

        @Override
        public LocationDistanceKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = pattern.matcher(keyString);
            if (matcher.matches()) {
                final String locationId = matcher.group(1);
                final String distanceString = matcher.group(2);
                final String uuidString = matcher.group(3);
                return new LocationDistanceKey(
                        locationId, Double.valueOf(distanceString),
                        UUID.fromString(uuidString));
            }
            throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
        }
    }
}
