/*
 * Copyright 2018 Public Transit Analytics.
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

import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetectorException;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class KnockoutEnvironmentDetector implements InEnvironmentDetector {
    
    private final Set<GeoBounds> knockedOut;

    @Override
    public boolean isOutOfBounds(final GeoPoint point) throws
            InEnvironmentDetectorException, InterruptedException {
        return knockedOut.stream().filter(bounds -> bounds.contains(point))
                .findAny().isPresent();
    }

    @Override
    public boolean isOutOfBounds(final GeoBounds bounds) throws
            InEnvironmentDetectorException, InterruptedException {
        return knockedOut.contains(bounds);
    }
    
}
