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

import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import lombok.Getter;
import com.publictransitanalytics.scoregenerator.visitors.Visitor;

/**
 *
 * @author Public Transit Analytics
 */
public class CountingVisitor implements Visitor {

    @Getter
    private int transitStopCount;
    @Getter
    private int landmarkCount;
    @Getter
    private int gridPointCount;

    @Override
    public void visit(TransitStop transitStop) {
        transitStopCount++;
    }

    @Override
    public void visit(Landmark point) {
        landmarkCount++;
    }

    @Override
    public void visit(GridPoint point) {
        landmarkCount++;
    }

}
