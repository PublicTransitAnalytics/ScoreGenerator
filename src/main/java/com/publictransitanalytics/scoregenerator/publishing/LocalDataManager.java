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
package com.publictransitanalytics.scoregenerator.publishing;

import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Gives a root path for a locally available set of files.
 *
 * @author Public Transit Analytics
 */
public class LocalDataManager implements DataManager {

    private final Path path;
    private final String baseConfiguration;
    private final Optional<String> comparisonConfiguration;

    public LocalDataManager(final String root, final String fileSet,
                            final String baseConfiguration,
                            final String comparisonConfiguration) {
        this(root, fileSet, baseConfiguration,
             Optional.of(comparisonConfiguration));
    }

    public LocalDataManager(final String root, final String fileSet,
                            final String baseConfiguration) {
        this(root, fileSet, baseConfiguration, Optional.empty());
    }

    private LocalDataManager(final String root, final String fileSet,
                             final String baseConfiguration,
                             final Optional<String> comparisonConfiguration) {
        path = Paths.get(root).resolve(fileSet);
        this.baseConfiguration = baseConfiguration;
        this.comparisonConfiguration = comparisonConfiguration;
    }

    @Override
    public Path getFileRoot() {
        return path;
    }

    @Override
    public InputStream getBaseConfiguration() throws PublicationException {
        try {
            return new FileInputStream(new File(baseConfiguration));
        } catch (FileNotFoundException e) {
            throw new PublicationException(e);
        }
    }

    @Override
    public Optional<InputStream> getComparisonConfiguration() throws
            PublicationException {
        try {
            return comparisonConfiguration.isPresent() ? Optional.of(
                    new FileInputStream(
                            new File(comparisonConfiguration.get()))) : Optional
                            .empty();
        } catch (FileNotFoundException e) {
            throw new PublicationException(e);
        }
    }

    @Override
    public void uploadFileSet(final String fileSet) {
    }

    @Override
    public void publish(final String outputName, final String output) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputName);
            writer.write(output);
        } catch (IOException e) {
            throw new ScoreGeneratorFatalException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                throw new ScoreGeneratorFatalException(e);
            }
        }
    }

}
