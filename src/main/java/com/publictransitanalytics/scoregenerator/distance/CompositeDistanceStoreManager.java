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
package com.publictransitanalytics.scoregenerator.distance;

import com.google.common.collect.ImmutableMap;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class CompositeDistanceStoreManager implements DistanceStoreManager {

    final Map<PointLocation, DistanceStoreManager> supplementalStores;
    final DistanceStoreManager defaultStore;

    @Override
    public Duration getMaxStored(final PointLocation location) throws
            InterruptedException {
        Duration max = defaultStore.getMaxStored(location);
        
        for (final DistanceStoreManager store : supplementalStores.values()) {
            final Duration supplementalMax = store.getMaxStored(location);
            if (max == null) {
                max = supplementalMax;
            } else if (supplementalMax != null) {
                if (supplementalMax.compareTo(max) > 0) {
                    max = supplementalMax;
                }
            }
            
        }
        return max;
    }

    @Override
    public void updateMaxStored(final PointLocation location,
                                final Duration duration)
            throws InterruptedException {

        if (supplementalStores.keySet().contains(location)) {
            final DistanceStoreManager store = supplementalStores.get(location);
            store.updateMaxStored(location, duration);
        } else {
            defaultStore.updateMaxStored(location, duration);
        }
    }

    @Override
    public Map<PointLocation, WalkingCosts> get(final PointLocation location,
                                                final Duration duration)
            throws InterruptedException {
        final ImmutableMap.Builder<PointLocation, WalkingCosts> builder
                = ImmutableMap.builder();
        builder.putAll(defaultStore.get(location, duration));
        for (final DistanceStoreManager store : supplementalStores.values()) {
            builder.putAll(store.get(location, duration));
        }
        return builder.build();
    }

    @Override
    public void putAll(final PointLocation location,
                       final Map<PointLocation, WalkingCosts> costs) throws
            InterruptedException {
        if (supplementalStores.keySet().contains(location)) {
            final DistanceStoreManager store = supplementalStores.get(location);
            store.putAll(location, costs);
        } else {
            final ImmutableMap.Builder<PointLocation, WalkingCosts> defaultCostsBuilder
                    = ImmutableMap.builder();
            for (final PointLocation destination : costs.keySet()) {
                if (supplementalStores.keySet().contains(destination)) {
                    final DistanceStoreManager store = supplementalStores.get(
                            location);
                    store.putAll(location, Collections.singletonMap(
                            destination, costs.get(destination)));
                } else {
                    defaultCostsBuilder.put(location, costs.get(destination));
                }
            }
            defaultStore.putAll(location, defaultCostsBuilder.build());

        }

    }

}
