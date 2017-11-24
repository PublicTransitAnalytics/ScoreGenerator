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
package com.publictransitanalytics.scoregenerator.datalayer.directories;

import com.bitvantage.bitvantagecaching.LmdbStore;
import com.bitvantage.bitvantagecaching.Store;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.WaterStatus;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.BoundsKey;
import com.publictransitanalytics.scoregenerator.geography.GeoJsonWaterDetector;
import com.publictransitanalytics.scoregenerator.geography.StoredWaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import com.publictransitanalytics.scoregenerator.geography.WaterDetectorException;
import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;

/**
 *
 * @author Public Transit Analytics
 */
public class EnvironmentDataDirectory {

    private static final String SEATTLE_OSM_FILE = "king_county.osm.pbf";
    private static final String GRAPHHOPPER_DIRECTORY = "graphhopper_files";

    private static final String WATER_STORE = "water_store";
    private static final String WATER_BODIES_FILE = "water.json";

    @Getter
    private final GraphHopper hopper;
    @Getter
    private final WaterDetector waterDetector;

    public EnvironmentDataDirectory(final Path root)
            throws IOException, WaterDetectorException {
        final Path osmFile = root.resolve(SEATTLE_OSM_FILE);
        final Path graphFolder = root.resolve(GRAPHHOPPER_DIRECTORY);

        hopper = new GraphHopperOSM().forServer();
        hopper.setDataReaderFile(osmFile.toString());
        hopper.setGraphHopperLocation(graphFolder.toString());
        hopper.setEncodingManager(new EncodingManager("foot"));
        hopper.setElevation(true);

        hopper.importOrLoad();

        final Store<BoundsKey, WaterStatus> waterStore = new LmdbStore<>(
                root.resolve(WATER_STORE), WaterStatus.class);

        final Path waterFilePath = root.resolve(WATER_BODIES_FILE);
        waterDetector = new StoredWaterDetector(
                new GeoJsonWaterDetector(waterFilePath), waterStore);
    }

}
