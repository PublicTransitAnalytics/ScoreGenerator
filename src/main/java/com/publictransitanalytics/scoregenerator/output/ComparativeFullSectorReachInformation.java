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

import com.publictransitanalytics.scoregenerator.tracking.MovementPath;

/**
 *
 * @author Public Transit Analytics
 */
public class ComparativeFullSectorReachInformation {

    private final SimplePath path;
    private final FullPath fullPath;
    private final SimplePath trialPath;
    private final FullPath trialFullPath;

    public ComparativeFullSectorReachInformation(
            final MovementPath movements, final MovementPath trialMovements)
            throws InterruptedException {
        path = (movements == null) ? null : new SimplePath(movements);
        fullPath = (movements == null) ? null : new FullPath(movements);
        trialPath = (trialMovements == null) ? null 
                : new SimplePath(trialMovements);
        trialFullPath = (trialMovements == null) ? null
                : new FullPath(trialMovements);
    }
}
