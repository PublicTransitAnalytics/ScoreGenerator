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

import com.google.common.collect.ImmutableSet;
import edu.emory.mathcs.backport.java.util.Collections;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class TarGzCompressorTest {

    private static final String ARCHIVE_NAME = "archive";
    private static final String FILE_NAME = "file name";
    private static final String FILE_CONTENTS = "contents";
    private static final String NESTED_DIRECTORY = "nested";
    private static final String SECOND_FILE_NAME = "second file name";
    private static final String SECOND_FILE_CONTENTS = "second file constants";
    private static final String NESTED_FILE_NAME = "nested file";
    private static final String NESTED_FILE_CONTENTS = "nested file contents";

    @Test
    public void testDecompressesEmpty() throws Exception {
        final TarGzCompressor compressor = new TarGzCompressor();
        final Path container
                = Files.createTempDirectory("testDecompressesEmpty");
        final byte[] data = {
            31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -19, -49, 77, 10, -62, 64, 12,
            -128, -47, 30, 101, 78, 96, 19, -57, 97, -50, 83, 68, -80, -37, -6,
            115, 126, 69, 23, 66, -63, -107, 118, -9, -34, 38, 89, -123, 47,
            -69, 113, 90, -114, -25, -7, 126, 26, -121, -51, 68, 28, -94, -73,
            86, -30, 109, 61, 95, 123, -42, -38, 34, -93, -11, -66, 47, -111,
            89, 123, 14, -91, 109, -105, -12, 113, -69, 92, -89, -27, -103, -8,
            -21, -99, -11, 115, 127, 72, 3, 0, 0, 0, 0, 0, 0, 0, 0, -128, 111,
            30, -73, -67, 86, 89, 0, 40, 0, 0};
        final ByteArrayInputStream stream = new ByteArrayInputStream(data);

        compressor.decompress(stream, container);
        stream.close();

        final String[] contents = container.toFile().list();
        Assert.assertEquals(1, contents.length);
        Assert.assertEquals(ARCHIVE_NAME, contents[0]);
    }

    @Test
    public void testPreservesEmpty() throws Exception {
        final TarGzCompressor compressor = new TarGzCompressor();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Path container = Files.createTempDirectory("testPreservesEmpty");
        final Path directory
                = Files.createDirectory(container.resolve(ARCHIVE_NAME));
        compressor.compress(stream, directory);
        stream.close();
        final Path newContainer
                = Files.createTempDirectory("testPreservesEmpty");
        final ByteArrayInputStream inputStream
                = new ByteArrayInputStream(stream.toByteArray());
        compressor.decompress(inputStream, newContainer);
        final String[] contents = newContainer.toFile().list();
        Assert.assertEquals(1, contents.length);
        Assert.assertEquals(ARCHIVE_NAME, contents[0]);
    }

    @Test
    public void testDecompressesContent() throws Exception {
        final TarGzCompressor compressor = new TarGzCompressor();
        final Path container
                = Files.createTempDirectory("testDecompressesContent");
        final byte[] data = {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -19, -105, -47,
            14, -126, 32, 20, -122, 125, 20, -98, 64, 15, 114, -64, -25, 113, 
            74, -53, -83, 104, 19, -22, -7, 35, 109, -23, 104, -51, -102, 66, 
            23, -98, -17, 6, -82, -28, -24, -65, -1, 83, -13, -94, -18, -101, 
            99, 119, -45, 69, 22, 13, 0, -124, 74, 74, 6, 35, -31, 58, -20, -71,
            16, 18, 68, 85, -126, 40, 25, 112, 46, 20, 102, 76, -58, 27, 105, 
            -30, 106, 93, -35, -5, 17, -41, 94, 39, -68, -71, 13, 70, 75, 65, 
            -2, -54, -33, -22, -26, 98, 90, 118, -24, 78, -102, -103, -6, -84, 
            -73, 59, 3, 56, -128, 66, -4, -100, 127, 41, -125, -4, 81, 72, -103,
            -79, -43, -103, 124, -61, -50, -13, -97, -89, -18, 119, -2, 97, 24,
            103, -1, 61, 20, -111, -116, -87, -1, 70, 91, -89, -37, 24, -81, 
            -127, -33, -3, 95, 42, 84, -28, -1, 20, -68, -27, 63, 46, -125, 16,
            -74, 58, 99, -39, -1, 24, -6, -65, 66, 36, -1, -89, 96, 22, -9, -61,
            -1, 78, -109, -2, 119, -59, -44, -1, 8, 31, 126, 79, 22, -5, -49, 
            67, -1, 11, -1, 7, 64, -3, 79, 1, 117, -98, 32, 8, 98, -97, -36, 1, 
            9, -113, -109, 38, 0, 20, 0, 0};
        final ByteArrayInputStream stream = new ByteArrayInputStream(data);

        compressor.decompress(stream, container);
        stream.close();
        
        final String[] contents = container.toFile().list();
        Assert.assertEquals(1, contents.length);
        Assert.assertEquals(ARCHIVE_NAME, contents[0]);
        final Path directory = container.resolve(ARCHIVE_NAME);
        final File directoryFile = directory.toFile();
        Assert.assertTrue(directoryFile.isDirectory());
        final String[] directoryContents = directoryFile.list();
        Assert.assertEquals(3, directoryContents.length);
        Assert.assertEquals(
                ImmutableSet.of(FILE_NAME, SECOND_FILE_NAME, NESTED_DIRECTORY),
                ImmutableSet.copyOf(directoryContents));
        final Path filePath = directory.resolve(FILE_NAME);
        Assert.assertTrue(filePath.toFile().exists());
        Assert.assertTrue(Arrays.equals(FILE_CONTENTS.getBytes(),
                                        Files.readAllBytes(filePath)));
        final Path secondFilePath = directory.resolve(SECOND_FILE_NAME);
        Assert.assertTrue(secondFilePath.toFile().exists());
        Assert.assertTrue(Arrays.equals(SECOND_FILE_CONTENTS.getBytes(),
                                        Files.readAllBytes(secondFilePath)));
        final Path nestedDirectoryPath
                = directory.resolve(NESTED_DIRECTORY);
        final File nestedDirectoryFile = nestedDirectoryPath.toFile();
        Assert.assertTrue(nestedDirectoryFile.isDirectory());
        final String[] nestedDirectoryContents
                = nestedDirectoryFile.list();
        Assert.assertEquals(1, nestedDirectoryContents.length);
        Assert.assertEquals(Collections.singleton(NESTED_FILE_NAME),
                            ImmutableSet.copyOf(nestedDirectoryContents));
        final Path nestedFilePath
                = nestedDirectoryPath.resolve(NESTED_FILE_NAME);
        Assert.assertTrue(Arrays.equals(NESTED_FILE_CONTENTS.getBytes(),
                                        Files.readAllBytes(nestedFilePath)));
    }

    @Test
    public void testPreservesContent() throws Exception {
        final TarGzCompressor compressor = new TarGzCompressor();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Path container
                = Files.createTempDirectory("testPreservesContent");
        final Path directory
                = Files.createDirectory(container.resolve(ARCHIVE_NAME));
        final Path filePath = directory.resolve(FILE_NAME);
        Files.write(filePath, FILE_CONTENTS.getBytes());
        final Path secondFilePath = directory.resolve(SECOND_FILE_NAME);
        Files.write(secondFilePath, SECOND_FILE_CONTENTS.getBytes());
        final Path nestedDirectoryPath
                = Files.createDirectory(directory.resolve(NESTED_DIRECTORY));
        final Path nestedFilePath
                = nestedDirectoryPath.resolve(NESTED_FILE_NAME);
        Files.write(nestedFilePath, NESTED_FILE_CONTENTS.getBytes());

        compressor.compress(stream, directory);
        stream.close();
        final Path newContainer
                = Files.createTempDirectory("testPreservesContent");
        final ByteArrayInputStream inputStream
                = new ByteArrayInputStream(stream.toByteArray());
        compressor.decompress(inputStream, newContainer);
        final String[] contents = newContainer.toFile().list();
        Assert.assertEquals(1, contents.length);
        Assert.assertEquals(ARCHIVE_NAME, contents[0]);
        final Path newDirectory = newContainer.resolve(ARCHIVE_NAME);
        final File newDirectoryFile = newDirectory.toFile();
        Assert.assertTrue(newDirectoryFile.isDirectory());
        final String[] directoryContents = newDirectoryFile.list();
        Assert.assertEquals(3, directoryContents.length);
        Assert.assertEquals(
                ImmutableSet.of(FILE_NAME, SECOND_FILE_NAME, NESTED_DIRECTORY),
                ImmutableSet.copyOf(directoryContents));
        final Path newFilePath = newDirectory.resolve(FILE_NAME);
        Assert.assertTrue(newFilePath.toFile().exists());
        Assert.assertTrue(Arrays.equals(FILE_CONTENTS.getBytes(),
                                        Files.readAllBytes(newFilePath)));
        final Path newSecondFilePath = newDirectory.resolve(SECOND_FILE_NAME);
        Assert.assertTrue(newSecondFilePath.toFile().exists());
        Assert.assertTrue(Arrays.equals(SECOND_FILE_CONTENTS.getBytes(),
                                        Files.readAllBytes(newSecondFilePath)));
        final Path newNestedDirectoryPath
                = newDirectory.resolve(NESTED_DIRECTORY);
        final File newNestedDirectoryFile = newNestedDirectoryPath.toFile();
        Assert.assertTrue(newNestedDirectoryFile.isDirectory());
        final String[] newNestedDirectoryContents
                = newNestedDirectoryFile.list();
        Assert.assertEquals(1, newNestedDirectoryContents.length);
        Assert.assertEquals(Collections.singleton(NESTED_FILE_NAME),
                            ImmutableSet.copyOf(newNestedDirectoryContents));
        final Path newNestedFilePath
                = newNestedDirectoryPath.resolve(NESTED_FILE_NAME);
        Assert.assertTrue(Arrays.equals(NESTED_FILE_CONTENTS.getBytes(),
                                        Files.readAllBytes(newNestedFilePath)));
    }

}
