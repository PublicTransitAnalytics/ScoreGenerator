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
package com.publictransitanalytics.scoregenerator.workflow;

import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.visitors.ModeInfo;
import java.time.LocalDateTime;
import lombok.NonNull;
import lombok.Value;

/**
 * A record in a dynamic programming table for finding paths.
 * 
 * @author Public Transit Analytics
 */
@Value
public class DynamicProgrammingRecord
        implements Comparable<DynamicProgrammingRecord> {

    @NonNull
    private LocalDateTime reachTime;
    @NonNull
    private ModeInfo mode;
    private VisitableLocation predecessor;

    @Override
    public int compareTo(DynamicProgrammingRecord other) {
        return reachTime.compareTo(other.reachTime);
    }
}
