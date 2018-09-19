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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 *
 * @author Public Transit Analytics
 */
public class TarGzCompressor implements Compressor {

    private static final String TAR_PATH_SEPARATOR_TEMPLATE = "%s/%s";

    @Override
    public Path decompress(final InputStream stream, final Path directory)
            throws CompressionException {
        try {
            final TarArchiveInputStream tarStream = new TarArchiveInputStream(
                    new GZIPInputStream(stream));
            ArchiveEntry entry = tarStream.getNextEntry();
            final Path root = directory.resolve(entry.getName());
            while (entry != null) {
                if (!tarStream.canReadEntryData(entry)) {
                    continue;
                }
                final Path path = directory.resolve(entry.getName());
                final File file = path.toFile();
                if (entry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new CompressionException(String.format(
                                "%s cannot be created.", file));
                    }
                } else {
                    final File parent = file.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new CompressionException(String.format(
                                "%s cannot be created.", parent));
                    }
                    Files.copy(tarStream, path);
                }
                entry = tarStream.getNextEntry();
            }
            return root;
        } catch (final IOException e) {
            throw new CompressionException(e);
        }
    }

    @Override
    public void compress(final OutputStream stream, final Path directory)
            throws CompressionException {
        try {
            final GZIPOutputStream gzipStream = new GZIPOutputStream(stream);
            final TarArchiveOutputStream tarStream
                    = new TarArchiveOutputStream(gzipStream);

            recursivelyAddToArchive(tarStream, directory.toFile(), ".");
            tarStream.close();
            gzipStream.close();
        } catch (final IOException e) {
            throw new CompressionException(e);
        }
    }

    private static void recursivelyAddToArchive(
            final TarArchiveOutputStream stream, final File file,
            final String archivePath) throws IOException {
        final String name = String.format(
                TAR_PATH_SEPARATOR_TEMPLATE, archivePath, file.getName());
        final TarArchiveEntry entry = new TarArchiveEntry(
                file, name);
        stream.putArchiveEntry(entry);
        if (file.isDirectory()) {
            stream.closeArchiveEntry();
            for (final File child : file.listFiles()) {
                recursivelyAddToArchive(stream, child, name);
            }
        } else if (file.isFile()) {
            Files.copy(file.toPath(), stream);
            stream.closeArchiveEntry();
        }
    }

}
