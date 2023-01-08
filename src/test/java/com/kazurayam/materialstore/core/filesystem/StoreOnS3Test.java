package com.kazurayam.materialstore.core.filesystem;

import com.kazurayam.materialstore.core.util.DeleteDir;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StoreOnS3Test {

    private final Logger logger = LoggerFactory.getLogger(StoreOnS3Test.class);
    private final String bucketName = "/com.kazurayam.materialstore.core.filesystem.store-on-s3-test";
    private static FileSystem s3fs;
    private static boolean CLEANUP_ON_END = false;
    private Path dir;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException, IOException {
        s3fs = FileSystems.newFileSystem(new URI("s3:///s3.ap-northeast-1.AMAZONAWS.COM/"),
                new HashMap<String, Object>(),
                Thread.currentThread().getContextClassLoader());
        assertNotNull(s3fs);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        s3fs.close();
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (CLEANUP_ON_END) {
            if (Files.exists(dir)) {
                DeleteDir.deleteDirectoryRecursively(dir);
            }
        }
    }

    @Test
    public void testS3fs() throws URISyntaxException, IOException {
        // create a directory in a S3 bucket if the directory is not present
        dir = s3fs.getPath(bucketName, "testS3fs");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        assertTrue(Files.exists(dir));

        // create a file in the dir
        Path file = dir.resolve("hello.txt");
        Files.write(file, "Hello, s3fs world!".getBytes());
        assertTrue(Files.exists(file));

        // list a directory
        assertEquals(1, Files.list(dir).count());

        // delete the file
        Files.delete(file);
        assertEquals(0, Files.list(dir).count());

        // delete the dir
        Files.delete(dir);
        assertFalse(Files.exists(dir));
    }

    @Test
    public void testCreateStore() throws IOException, MaterialstoreException {
        // create a directory in a S3 bucket if the directory is not present
        dir = s3fs.getPath(bucketName, "testCreateStore");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        assertTrue(Files.exists(dir));
        Path root = dir.resolve("store");
        Files.createDirectories(root);
        // create the store
        Store store = Stores.newInstance(root);

        // write a material into the store
        JobName jobName = new JobName("foo");
        JobTimestamp jobTimestamp = JobTimestamp.now();
        Metadata metadata = Metadata.NULL_OBJECT;
        Material material =
                store.write(jobName, jobTimestamp, FileType.TXT, metadata, "Hello, world!");
        assertTrue(Files.exists(material.toPath()));
    }

    @Test
    public void testNewInstanceOnAwsS3() {
        dir = s3fs.getPath(bucketName, "testCreateStore");
    }
}
