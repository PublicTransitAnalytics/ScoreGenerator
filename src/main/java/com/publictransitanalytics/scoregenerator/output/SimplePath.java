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
package com.publictransitanalytics.scoregenerator.output;

import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;

/**
 *
 * @author Public Transit Analytics
 */
@EqualsAndHashCode
public class SimplePath {

    private final String pathString;

    public SimplePath(final MovementPath path) {
        pathString = String.join(
                " => ", path.getMovements().stream().map(
                        Movement::getShortForm).collect(
                        Collectors.toList()));

    }

    @Override
    public String toString() {
        return pathString;
    }

}
