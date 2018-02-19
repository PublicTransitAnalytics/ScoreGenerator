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
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.BoundsStatus;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.BoundsKey;
import com.publictransitanalytics.scoregenerator.geography.GeoJsonInEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.geography.StoredInEnvironmentDetector;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetectorException;
import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetector;

/**
 *
 * @author Public Transit Analytics
 */
public class EnvironmentDataDirectory {

    private static final String OSM_FILE = "environment.osm.pbf";
    private static final String GRAPHHOPPER_DIRECTORY = "graphhopper_files";

    private static final String ENVIRONMENT_BOUNDS_STORE = "bounds_store";
    private static final String WATER_BODIES_FILE = "water.json";
    private static final String BORDER_FILE = "border.json";

    @Getter
    private final GraphHopper hopper;
    @Getter
    private final InEnvironmentDetector detector;
    @Getter
    private final Path osmPath;

    public EnvironmentDataDirectory(final Path root)
            throws IOException, InEnvironmentDetectorException {
        osmPath = root.resolve(OSM_FILE);
        final Path graphFolder = root.resolve(GRAPHHOPPER_DIRECTORY);

        hopper = new GraphHopperOSM().forServer();
        hopper.setDataReaderFile(osmPath.toString());
        hopper.setGraphHopperLocation(graphFolder.toString());
        hopper.setEncodingManager(new EncodingManager("foot"));
        hopper.setElevation(true);
        hopper.importOrLoad();

        final Store<BoundsKey, BoundsStatus> waterStore = new LmdbStore<>(
                root.resolve(ENVIRONMENT_BOUNDS_STORE), BoundsStatus.class);

        final Path borderFilePath = root.resolve(BORDER_FILE);
        final Path waterFilePath = root.resolve(WATER_BODIES_FILE);

        detector = new StoredInEnvironmentDetector(
                new GeoJsonInEnvironmentDetector(borderFilePath, waterFilePath),
                waterStore);
    }

}
