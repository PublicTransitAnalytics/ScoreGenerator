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
package com.publictransitanalytics.scoregenerator.tracking;

import com.google.common.collect.ImmutableList;

/**
 * Models a path that is constructed as the rider moves forward through time.
 * 
 * @author Public Transit Analytics
 */
public class ForwardMovingPath extends MovementPath {

    public ForwardMovingPath(final ImmutableList<Movement> movements) {
        super(movements);
    }

    @Override
    public MovementPath makeAppended(Movement movement) {
        return new ForwardMovingPath(ImmutableList.<Movement>builder()
                .addAll(movements).add(movement).build());
    }
    
}
