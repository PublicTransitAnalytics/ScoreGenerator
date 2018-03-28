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

import com.bitvantage.bitvantagecaching.DummySerializer;
import com.bitvantage.bitvantagecaching.Key;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.NativeLmdbStore;
import com.bitvantage.bitvantagecaching.RangedKey;
import com.bitvantage.bitvantagecaching.StoreBackedRangedKeyStore;
import com.bitvantage.bitvantagecaching.RangedNativeLmdbStore;
import com.bitvantage.bitvantagecaching.RangedStore;
import com.bitvantage.bitvantagecaching.Serializer;
import com.bitvantage.bitvantagecaching.Store;
import java.nio.file.Path;

/**
 *
 * @author Public Transit Analytics
 */
public class NoCacheStoreFactory implements StoreFactory {

    @Override
    public <K extends Key, V> Store<K, V> getStore(
            final Path path, final Serializer<V> serializer) {
        final int readers = Runtime.getRuntime().availableProcessors();
        return new NativeLmdbStore<>(path, serializer, readers);
    }

    @Override
    public <K extends RangedKey<K>, V> RangedStore getRangedStore(
            final Path path, final KeyMaterializer<K> keyMaterializer,
            final Serializer<V> serializer) {
        final int readers = Runtime.getRuntime().availableProcessors();
        return new RangedNativeLmdbStore<>(path, keyMaterializer, serializer,
                                           readers);
    }

    @Override
    public <K extends RangedKey<K>> StoreBackedRangedKeyStore getRangedKeyStore(
            final Path path, final KeyMaterializer<K> keyMaterializer) {
        return new StoreBackedRangedKeyStore<>(getRangedStore(
                path, keyMaterializer, new DummySerializer()));
    }

}
