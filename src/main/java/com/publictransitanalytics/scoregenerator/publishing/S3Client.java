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

import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Public Transit Analytics
 */
public class S3Client implements RemoteClient {

    private static final String REGION = "us-east-2";

    private final AmazonS3 s3Client;
    private final Compressor compressor;
    private final String bucketName;
    private final Path tempDir;

    public S3Client(final Compressor compressor, final String bucketName)
            throws DownloaderException {
        s3Client = AmazonS3ClientBuilder.standard().withRegion(REGION)
                .withCredentials(new EC2ContainerCredentialsProviderWrapper())
                .build();
        this.compressor = compressor;
        this.bucketName = bucketName;
        try {
            tempDir = Files.createTempDirectory("ScoreGenerator");
        } catch (final IOException e) {
            throw new DownloaderException(e);
        }
    }

    @Override
    public Path downloadFileSet(final String fileSet) 
            throws DownloaderException {
        final String key = String.format("%s.tar.gz", fileSet);
        final S3ObjectInputStream contentStream = getContentStream(key);

       
        try {
            return compressor.decompress(contentStream, tempDir);
        } catch (final CompressionException e) {
            throw new DownloaderException(e);
        }
    }
    
    @Override
    public InputStream getConfiguration(final String configurationName) {
        return getContentStream(configurationName);
    }

    @Override
    public void saveFileSet(final String fileSet) throws DownloaderException {
        final String key = String.format("%s.tar.gz", fileSet);

        try {
            final ByteArrayOutputStream data = new ByteArrayOutputStream();
            final Path root = tempDir.resolve(fileSet);
            compressor.compress(data, root);
            final ByteArrayInputStream inputStream
                    = new ByteArrayInputStream(data.toByteArray());
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.size());
            s3Client.putObject(bucketName, key, inputStream, metadata);
        } catch (final CompressionException e) {
            throw new DownloaderException(e);
        }
    }

    @Override
    public void uploadText(final String name, final String data)
            throws DownloaderException {
        s3Client.putObject(bucketName, name, data);
    }
    
    private S3ObjectInputStream getContentStream(final String key) {
         final S3Object object = s3Client.getObject(
                new GetObjectRequest(bucketName, key));
        return object.getObjectContent();
    }

}
