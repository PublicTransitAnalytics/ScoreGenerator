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

import com.publictransitanalytics.scoregenerator.location.PointLocation;
import java.time.LocalDateTime;
import lombok.Value;

/**
 * The identifying information for passing to workflows.
 * 
 * @author Public Transit Analytics
 */
@Value
public class TaskIdentifier {
    private final LocalDateTime time;
    private final PointLocation center;
}