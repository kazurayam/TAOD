package com.kazurayam.materialstore.store


import com.kazurayam.materialstore.TestFixtureUtil
import com.kazurayam.materialstore.store.differ.ImageDifferToPNG
import com.kazurayam.materialstore.store.reporter.DiffReporterToHTML
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.*

class DifferDriverImplTest {

    private static Path outputDir =
            Paths.get(".").resolve("build/tmp/testOutput")
                    .resolve(DifferDriverImplTest.class.getName())

    private static Path resultsDir =
            Paths.get(".").resolve("src/test/resources/fixture/sample_results")

    @BeforeAll
    static void beforeAll() {
        if (Files.exists(outputDir)) {
            FileUtils.deleteDirectory(outputDir.toFile())
        }
        Files.createDirectories(outputDir)
    }

    @Test
    void test_ImageDiffer() {
        Path root = outputDir.resolve("Materials")
        StoreImpl storeImpl = new StoreImpl(root)
        assert Files.exists(root)
        JobName jobName = new JobName("test_ImageDiffer")
        TestFixtureUtil.setupFixture(storeImpl, jobName)
        JobTimestamp jobTimestamp = new JobTimestamp("20210715_145922")
        //
        List<Material> expected = storeImpl.select(jobName, jobTimestamp,
                new MetadataPattern(["profile": "ProductionEnv"]), FileType.PNG)

        List<Material> actual = storeImpl.select(jobName, jobTimestamp,
                new MetadataPattern(["profile": "DevelopmentEnv"]), FileType.PNG)

        List<DiffArtifact> input =
                storeImpl.zipMaterials(expected, actual, ["URL.file"] as Set)
        //
        DifferDriver differDriver = new DifferDriverImpl.Builder(root).build()
        List<DiffArtifact> stuffed = differDriver.makeDiffArtifacts(input)
        assertNotNull(stuffed)
        assertEquals(1, stuffed.size())
    }

    @Test
    void test_Builder_differFor() {
        Path root = outputDir.resolve("Materials")
        Store store = new StoreImpl(root)
        DifferDriver differDriver = new DifferDriverImpl.Builder(root)
                .differFor(FileType.JPEG, new ImageDifferToPNG())
                .build()
        assertTrue(differDriver.hasDiffer(FileType.JPEG))
    }

    @Test
    void test_TextDiffer() {
        Path root = outputDir.resolve("Materials")
        StoreImpl storeImpl = new StoreImpl(root)
        JobName jobName = new JobName("test_TextDiffer")
        TestFixtureUtil.setupFixture(storeImpl, jobName)
        JobTimestamp jobTimestamp = new JobTimestamp("20210715_145922")
        //
        List<Material> expected = storeImpl.select(jobName, jobTimestamp,
                new MetadataPattern(["profile": "ProductionEnv"]), FileType.HTML)

        List<Material> actual = storeImpl.select(jobName, jobTimestamp,
                new MetadataPattern(["profile": "DevelopmentEnv"]), FileType.HTML)

        List<DiffArtifact> input =
                storeImpl.zipMaterials(expected, actual, ["URL.file"] as Set)
        //
        DifferDriver differDriver = new DifferDriverImpl.Builder(root).build()
        List<DiffArtifact> stuffed = differDriver.makeDiffArtifacts(input)
        assertNotNull(stuffed)
        assertEquals(1, stuffed.size())
    }

    static class DiffReporterToHTMLTest {

        private static Path outputDir =
                Paths.get(".").resolve("build/tmp/testOutput")
                        .resolve(DiffReporterToHTMLTest.class.getName())

        private static Path resultsDir =
                Paths.get(".").resolve("src/test/resources/fixture/sample_results")

        @BeforeAll
        static void beforeAll() {
            if (Files.exists(outputDir)) {
                FileUtils.deleteDirectory(outputDir.toFile())
            }
            Files.createDirectories(outputDir)
        }

        @Test
        void test_reportDiffs() {
            Path root = outputDir.resolve("Materials")
            Store store = new StoreImpl(root)
            assert Files.exists(root)
            JobName jobName = new JobName("test_reportDiffs")
            TestFixtureUtil.setupFixture(store, jobName)
            JobTimestamp jobTimestamp = new JobTimestamp("20210715_145922")
            //
            List<Material> expected = store.select(jobName, jobTimestamp,
                    new MetadataPattern(["profile": "ProductionEnv"]))

            List<Material> actual = store.select(jobName, jobTimestamp,
                    new MetadataPattern(["profile": "DevelopmentEnv"]))

            List<DiffArtifact> input =
                    store.zipMaterials(expected, actual, ["URL.file", "category"] as Set)

            // we are going to build a DifferDriver
            DifferDriver differDriver = new DifferDriverImpl.Builder(root).build()

            // now make the diffs
            List<DiffArtifact> stuffed = differDriver.makeDiffArtifacts(input)
            assertNotNull(stuffed)
            assertEquals(2, stuffed.size())
            //
            DiffReporter diffReporter = new DiffReporterToHTML(root, jobName)
            Path reportFile = root.resolve("index.html")
            diffReporter.reportDiffs(stuffed, reportFile.getFileName().toString())
            //
            assertTrue(Files.exists(reportFile))
            assertTrue(reportFile.toFile().length() > 0)
        }


    }
}
