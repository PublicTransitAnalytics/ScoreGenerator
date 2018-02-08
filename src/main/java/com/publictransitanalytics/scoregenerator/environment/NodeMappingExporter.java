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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class NodeMappingExporter implements Sink {

    private final ImmutableMap.Builder<Long, Node> builder;

    public NodeMappingExporter() {
        builder = ImmutableMap.builder();
    }

    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer  instanceof  NodeContainer) {
        final NodeContainer nodeContainer = (NodeContainer) entityContainer;
            final Node node = nodeContainer.getEntity();
            builder.put(node.getId(), node);
        } else {
            log.debug("Found container {} in node list.", entityContainer);
        }
    }

    @Override
    public void initialize(Map<String, Object> metaData) {
    }

    @Override
    public void complete() {
    }

    @Override
    public void release() {
    }

    public Map<Long, Node> getMapping() {
        return builder.build();
    }

}
