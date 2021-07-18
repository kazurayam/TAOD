package com.kazurayam.materialstore.diff

import com.kazurayam.materialstore.TestFixtureUtil
import com.kazurayam.materialstore.store.FileType
import com.kazurayam.materialstore.store.JobName
import com.kazurayam.materialstore.store.JobTimestamp
import com.kazurayam.materialstore.store.Material
import com.kazurayam.materialstore.store.MetadataPattern
import com.kazurayam.materialstore.store.StoreImpl
import org.junit.jupiter.api.Test

import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.*

class ImageDifferTest {

    private static Path outputDir =
            Paths.get(".").resolve("build/tmp/testOutput")
                    .resolve(DifferDriverTest.class.getName())

    private static Path resultsDir =
            Paths.get(".").resolve("src/test/resources/fixture/sample_results")


    @Test
    void test_makeDiff() {
        Path root = outputDir.resolve("Materials")
        StoreImpl storeImpl = new StoreImpl(root)
        JobName jobName = new JobName("test_makeDiff")
        JobTimestamp jobTimestamp = new JobTimestamp("20210715_145922")
        TestFixtureUtil.setupFixture(storeImpl, jobName)
        //
        List<Material> expected = storeImpl.select(jobName, jobTimestamp, FileType.PNG,
                new MetadataPattern(["profile": "ProductionEnv"]))

        List<Material> actual = storeImpl.select(jobName, jobTimestamp, FileType.PNG,
                new MetadataPattern(["profile": "DevelopmentEnv"]))

        List<DiffArtifact> diffArtifacts =
                storeImpl.zipMaterials(expected, actual, ["URL.file"] as Set)
        assertNotNull(diffArtifacts)
        assertEquals(1, diffArtifacts.size())
        //
        DiffArtifact stuffed = new ImageDiffer(root).makeDiff(diffArtifacts[0])
        assertNotNull(stuffed)
        assertNotNull(stuffed.getDiff())
        assertNotEquals(Material.NULL_OBJECT, stuffed.getDiff())
    }
}