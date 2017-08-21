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
package com.publictransitanalytics.scoregenerator.visitors;

import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import lombok.NonNull;
import lombok.Value;

/**
 * The description of a trip taken.
 * 
 * @author Public Transit Analytics
 */
@Value
public class ModeInfo {
    
    public static final ModeInfo NONE = new ModeInfo(ModeType.NONE, null, null);
    
    @NonNull
    private final ModeType type;
    private final EntryPoint transitTrip;
    private final WalkingCosts walkCosts;
    
}
