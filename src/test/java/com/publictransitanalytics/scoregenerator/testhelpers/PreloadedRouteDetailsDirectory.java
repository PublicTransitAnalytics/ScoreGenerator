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
package com.publictransitanalytics.scoregenerator.testhelpers;

import com.publictransitanalytics.scoregenerator.datalayer.directories.RouteDetailsDirectory;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.RouteDetails;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class PreloadedRouteDetailsDirectory implements RouteDetailsDirectory {

    final Map<String, RouteDetails> map;

    @Override
    public RouteDetails getRouteDetails(final String routeId) {
        return map.get(routeId);
    }

}
