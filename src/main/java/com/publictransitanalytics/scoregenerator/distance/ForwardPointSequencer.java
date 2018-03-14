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

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class ForwardPointSequencer implements PointSequencer {

    final PointLocation origin;
    final Set<PointLocation> destinations;

    @Override
    public List<PointLocation> getSequence() {
        return ImmutableList.<PointLocation>builder().addAll(destinations)
                .add(origin).build();
    }

    @Override
    public Set<Integer> getOrigins() {
        return Collections.singleton(destinations.size());
    }

    @Override
    public Set<Integer> getDestinations() {
        return IntStream.range(0, destinations.size()).boxed().collect(
                Collectors.toSet());
    }

}
