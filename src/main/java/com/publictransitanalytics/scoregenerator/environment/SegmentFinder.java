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
package com.publictransitanalytics.scoregenerator.environment;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.GeoBounds;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.openstreetmap.osmosis.areafilter.v0_6.AreaFilter;
import org.openstreetmap.osmosis.areafilter.v0_6.BoundingBoxFilter;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;
import org.openstreetmap.osmosis.tagfilter.v0_6.TagFilter;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class SegmentFinder {

    private static final String WAY_FILTER_MODE = "accept-ways";
    private static final String HIGHWAY_FILTER_MODE = "reject-ways";
    private static final String NODE_FILTER_MODE = "accept-nodes";

    private final File file;
    private final GeoBounds bounds;

    public Set<Segment> getSegments() {
        return readSegments(readNodes());
    }

    private Set<Segment> readSegments(final Map<Long, Node> nodeMapping) {
        final PbfReader reader = new PbfReader(file, 1);
        final AreaFilter areaFilter = new BoundingBoxFilter(
                IdTrackerType.Dynamic, bounds.getWestLon().getDegrees(),
                bounds.getEastLon().getDegrees(),
                bounds.getNorthLat().getDegrees(),
                bounds.getSouthLat().getDegrees(), true, true, false, false);
        final TagFilter wayFilter = new TagFilter(
                WAY_FILTER_MODE,
                ImmutableSet.of("footway", "highway", "access"),
                ImmutableMap.of("man_made", Collections.singleton("pier")));
        final TagFilter noWalkFilter = new TagFilter(
                HIGHWAY_FILTER_MODE, Collections.emptySet(),
                ImmutableMap.of("foot", Collections.singleton("no"), "highway",
                                ImmutableSet.of("motorway", "motorway_link")));
        final SegmentExporter exporter = new SegmentExporter(nodeMapping);
        reader.setSink(wayFilter);
        wayFilter.setSink(noWalkFilter);
        noWalkFilter.setSink(areaFilter);
        areaFilter.setSink(exporter);

        reader.run();

        return exporter.getSegments();
    }

    private Map<Long, Node> readNodes() {
        final PbfReader reader = new PbfReader(file, 1);
        final AreaFilter areaFilter = new BoundingBoxFilter(
                IdTrackerType.Dynamic, bounds.getWestLon().getDegrees(),
                bounds.getEastLon().getDegrees(),
                bounds.getNorthLat().getDegrees(),
                bounds.getSouthLat().getDegrees(), true, true, false, false);
        final TagFilter nodeFilter = new TagFilter(NODE_FILTER_MODE,
                                                   Collections.emptySet(),
                                                   Collections.emptyMap());
        final NodeMappingExporter exporter = new NodeMappingExporter();
        reader.setSink(nodeFilter);
        nodeFilter.setSink(areaFilter);
        areaFilter.setSink(exporter);

        reader.run();

        return exporter.getMapping();
    }

}
