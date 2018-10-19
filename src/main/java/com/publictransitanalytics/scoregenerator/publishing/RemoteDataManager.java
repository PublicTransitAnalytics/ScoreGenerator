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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Downloads files for ScoreGeneration and gives their location.
 *
 * @author Public Transit Analytics
 */
public class RemoteDataManager implements DataManager {

    private final RemoteClient client;
    private final Path root;
    private final String baseConfiguration;
    private final Optional<String> comparisonConfiguration;

    public RemoteDataManager(final RemoteClient client,
                             final String fileSet,
                             final String baseConfiguration) {
        this(client, fileSet, baseConfiguration, Optional.empty());
    }

    public RemoteDataManager(final RemoteClient client,
                             final String fileSet,
                             final String baseConfiguration,
                             final String comparisonConfiguration) {
        this(client, fileSet, baseConfiguration,
             Optional.of(comparisonConfiguration));
    }

    private RemoteDataManager(final RemoteClient client,
                              final String fileSet,
                              final String baseConfiguration,
                              final Optional<String> comparisonConfiguration) {
        this.client = client;
        try {
            root = client.downloadFileSet(fileSet);
        } catch (final DownloaderException e) {
            throw new ScoreGeneratorFatalException(e);
        }
        this.baseConfiguration = baseConfiguration;
        this.comparisonConfiguration = comparisonConfiguration;
    }

    @Override
    public Path getFileRoot() {
        return root;
    }

    @Override
    public InputStream getBaseConfiguration() throws PublicationException {
        return client.getConfiguration(baseConfiguration);
    }

    @Override
    public Optional<InputStream> getComparisonConfiguration()
            throws PublicationException {

        return comparisonConfiguration.isPresent() ? Optional.of(client
                .getConfiguration(baseConfiguration)) : Optional.empty();
    }

    @Override
    public void uploadFileSet(final String fileSet) {
        try {
            client.saveFileSet(fileSet);
        } catch (final DownloaderException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public void publish(final String name, final String data) {
        try {
            client.uploadText(name, data);
        } catch (DownloaderException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

}
