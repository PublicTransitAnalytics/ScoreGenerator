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
package com.publictransitanalytics.scoregenerator.datalayer.scoring;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class StoreMappingKeyTest {

    @Test
    public void testMinKey() {
        final ScoreMappingKey key = ScoreMappingKey.getMinKey("originId");
        Assert.assertEquals("originId::0000-01-01T00:00::", key.getKeyString());
    }

    @Test
    public void testMaxKey() {
        final ScoreMappingKey key = ScoreMappingKey.getMaxKey("originId");
        Assert.assertEquals("originId::9999-12-31T23:59::", key.getKeyString());
    }

}
