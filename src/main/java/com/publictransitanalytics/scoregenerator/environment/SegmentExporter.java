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

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.AngleUnit;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class SegmentExporter implements Sink {

    private final ImmutableSet.Builder builder;
    private final Map<Long, Node> nodes;

    public SegmentExporter(final Map<Long, Node> nodes) {
        builder = ImmutableSet.builder();
        this.nodes = nodes;
    }

    @Override
    public void process(final EntityContainer entityContainer) {
        if (entityContainer instanceof WayContainer) {
            final WayContainer wayContainer = (WayContainer) entityContainer;
            final Way way = wayContainer.getEntity();
            final boolean closed = way.isClosed();
            final List<WayNode> wayNodes = way.getWayNodes();
            final Iterator<WayNode> iterator = wayNodes.iterator();
            final long wayId = way.getId();
            final WayNode first = iterator.next();
            WayNode beginning = first;
            int i = 0;
            while (iterator.hasNext()) {
                final WayNode end = iterator.next();

                final Segment segment = makeSegment(beginning, end, wayId, i);
                if (segment != null) {
                    builder.add(segment);
                }
                beginning = end;
                i++;
            }
            if (closed) {
                final Segment closingSegment
                        = makeSegment(wayNodes.get(wayNodes.size() - 1), first,
                                      wayId, i);
                if (closingSegment != null) {
                    builder.add(closingSegment);
                }
            }
        }
    }

    private Segment makeSegment(final WayNode beginning, final WayNode end,
                                long wayId, int nodeIndex) {
        final Node beginningNode = nodes.get(beginning.getNodeId());
        final Node endNode = nodes.get(end.getNodeId());

        if (beginningNode == null || endNode == null) {
            log.debug("could not make segment beginningNode={} endNode={}",
                      beginningNode, endNode);
            return null;
        }
        log.debug("Making segment beginningNode={} endNode={}", beginningNode,
                  endNode);
        final GeoPoint beginningPoint = new GeoPoint(
                new GeoLongitude(
                        String.valueOf(beginningNode.getLongitude()),
                        AngleUnit.DEGREES),
                new GeoLatitude(
                        String.valueOf(beginningNode.getLatitude()),
                        AngleUnit.DEGREES));
        final GeoPoint endPoint = new GeoPoint(
                new GeoLongitude(String.valueOf(endNode.getLongitude()),
                                 AngleUnit.DEGREES),
                new GeoLatitude(String.valueOf(endNode.getLatitude()),
                                AngleUnit.DEGREES));

        return new Segment(beginningPoint, endPoint, wayId, nodeIndex);

    }

    @Override
    public void initialize(final Map<String, Object> metaData) {
    }

    @Override
    public void complete() {
    }

    @Override
    public void close() {
    }

    public Set<Segment> getSegments() {
        return builder.build();
    }

}
