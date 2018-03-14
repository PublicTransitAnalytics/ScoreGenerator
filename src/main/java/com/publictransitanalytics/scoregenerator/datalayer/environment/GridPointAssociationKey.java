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
package com.publictransitanalytics.scoregenerator.datalayer.environment;

import com.bitvantage.bitvantagecaching.BitvantageStoreException;
import com.bitvantage.bitvantagecaching.KeyMaterializer;
import com.bitvantage.bitvantagecaching.RangedKey;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.Sector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GridPointAssociationKey
        extends RangedKey<GridPointAssociationKey> {

    @Getter
    private final String keyString;
    private final String gridId;

    public GridPointAssociationKey(final String gridId,
                                   final GridPoint gridPoint,
                                   final Sector associatedSector) {
        this(gridId, gridPoint.getIdentifier(),
             associatedSector.getIdentifier());
    }

    private GridPointAssociationKey(final String gridId,
                                    final String gridPointString,
                                    final String sectorString) {
        keyString = String.format("%s::%s::%s",
                                  gridId, gridPointString, sectorString);
        this.gridId = gridId;
    }

    @Override
    public GridPointAssociationKey getRangeMin() {
        return getMinKey(gridId);
    }

    @Override
    public GridPointAssociationKey getRangeMax() {
        return getMaxKey(gridId);
    }

    public static GridPointAssociationKey getMinKey(final String gridId) {
        return new GridPointAssociationKey(String.format("%s::", gridId),
                                           gridId);
    }

    public static GridPointAssociationKey getMaxKey(final String gridId) {
        return new GridPointAssociationKey(String.format("%s:;", gridId),
                                           gridId);
    }

    public static class Materializer implements
            KeyMaterializer<GridPointAssociationKey> {

        private static final Pattern PATTERN
                = Pattern.compile("(.+)::(.+)::(.+)");

        @Override
        public GridPointAssociationKey materialize(final String keyString)
                throws BitvantageStoreException {
            final Matcher matcher = PATTERN.matcher(keyString);
            if (matcher.matches()) {
                final String gridId = matcher.group(1);
                final String gridPointString = matcher.group(2);
                final String sectorString = matcher.group(3);
                return new GridPointAssociationKey(gridId, gridPointString,
                                                  sectorString);
            } else {
                 throw new BitvantageStoreException(String.format(
                    "Key string %s could not be materialized", keyString));
            }
        }
    }

}
