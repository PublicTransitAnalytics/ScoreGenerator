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
package com.publictransitanalytics.scoregenerator.schedule;

import java.time.LocalDateTime;
import java.time.Month;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class EntryPointTimeKeyTest {

    @Test
    public void testUUIDBreaksTie() {
        final LocalDateTime time = LocalDateTime.of(2017, Month.OCTOBER, 13,
                                                    9, 33);
        final EntryPointTimeKey maxKey = EntryPointTimeKey.getMaximalKey(time);
        final EntryPointTimeKey minKey = EntryPointTimeKey.getMinimalKey(time);
        Assert.assertTrue(minKey.compareTo(maxKey) < 0);
    }

}
