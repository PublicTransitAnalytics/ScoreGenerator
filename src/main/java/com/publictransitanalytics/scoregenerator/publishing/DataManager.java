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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 *
 * @author Public Transit Analytics
 */
public interface DataManager {

    public Path getFileRoot();

    public InputStream getBaseConfiguration() throws PublicationException;

    public Optional<InputStream> getComparisonConfiguration()
            throws PublicationException;

    public void uploadFileSet(String fileSet);

    public void publish(String outputName, String output);

}
