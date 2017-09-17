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
package com.publictransitanalytics.scoregenerator;

import com.bitvantage.bitvantagecaching.Key;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Store;
import java.nio.file.Path;

/**
 *
 * @author Public Transit Analytics
 */
public interface StoreFactory {

    <K extends Key, V> Store<K, V> getStore(Path path, Class<V> valueClass);

    <K extends RangedKey<K>, V> RangedStore getRangedStore(
            Path path, KeyMaterializer<K> keyMaterializer, Class<V> valueClass);

}
