package com.kazurayam.materials.demo

import com.kazurayam.materials.diff.DiffArtifact
import com.kazurayam.materials.diff.Differ
import com.kazurayam.materials.diff.Reporter
import com.kazurayam.materials.selenium.AShotWrapper
import com.kazurayam.materials.store.FileType
import com.kazurayam.materials.store.Material
import com.kazurayam.materials.store.MetadataJoint
import com.kazurayam.materials.store.Store
import com.kazurayam.materials.store.JobName
import com.kazurayam.materials.store.JobTimestamp

import com.kazurayam.materials.store.Metadata
import com.kazurayam.materials.store.MetadataPattern
import com.kazurayam.materials.store.StoreImpl
import com.kazurayam.materials.store.Stores
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class VisualTestingTwins {

    private Path root_ = Paths.get("./build/tmp/demoOutput/${VisualTestingTwins.class.getSimpleName()}/Materials")

    void setRoot(Path root) {
        this.root_ = root
    }

    void init() {
        if (Files.exists(root_)) {
            // delete the directory to clear out
            Files.walk(root_)
                    .sorted(Comparator.reverseOrder())
                    .map {it.toFile() }
                    .forEach {it.delete() }
        }
        Files.createDirectories(root_)
    }

    static WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(120, TimeUnit.MILLISECONDS);
        driver.manage().window().setSize(new Dimension(800, 800));
        return driver
    }

    void execute() {
        init()
        Store store = Stores.newInstance(root_)
        JobName jobName = new JobName("VisualTestingTwins")
        JobTimestamp jobTimestamp = JobTimestamp.now()
        // open the Chrome browser
        WebDriver driver = createChromeDriver()

        // visit the 1st page
        String profile1 = "ProductionEnv"
        URL url1 = new URL("http://demoaut.katalon.com/")
        Tuple result1 = doAction(driver, store, jobName, jobTimestamp, profile1, url1)
        assert result1 != null

        // visit the 2nd page
        String profile2 = "DevelopmentEnv"
        URL url2 = new URL("http://demoaut-mimic.kazurayam.com/")
        Tuple result2 = doAction(driver, store, jobName, jobTimestamp, profile2, url2)
        assert result2 != null

        // close the Chrome browser
        driver.quit()


        // pickup the screenshots that belongs to the 2 "profiles", make image-diff files of each.
        List<Material> screenshotsOfProfile1 = store.select(jobName, jobTimestamp,
                FileType.PNG, new MetadataPattern([ profile1 ]))
        List<Material> screenshotsOfProfile2 = store.select(jobName, jobTimestamp,
                FileType.PNG, new MetadataPattern([ profile2 ]))


        List<DiffArtifact> materialPairsToDiff = store.zipMaterialsToDiff(
                jobName, jobTimestamp, FileType.PNG,
                screenshotsOfProfile1,
                screenshotsOfProfile2,
                new MetadataJoint("", "+"))

        /*

        // make imageDiffs and save them into disk,
        // returns the list of DiffResult with the diff property stuffed
        Differ differ = store.newDiffer(jobName, jobTimestamp)
        List<DiffArtifact> diffArtifacts = differ.makeDiff(materialPairsToDiff)

        // compile HTML report
        Reporter reporter = store.newReporter(jobName, jobTimestamp)
        Path reportFile = store.getRoot().resolve("index.html")
        reporter.report(diffArtifacts, reportFile)

         */
    }

    private static Tuple doAction(WebDriver driver,
                           Store store, JobName jobName, JobTimestamp jobTimestamp,
                           String profile, URL url) {
        // visit the page
        driver.navigate().to(url.toString())

        // Metadata("ProductionEnv", "http://demoaut.katalon.com/", "demoaut.katalon.com", "/")
        Metadata metadata = new Metadata(profile,
                url.toExternalForm(),
                url.getHost(),
                url.getFile()
        )

        // take and store the PNG screenshot of the page
        BufferedImage image = AShotWrapper.takeEntirePageImage(driver)
        Material imageMaterial = store.write(jobName, jobTimestamp, FileType.PNG, metadata, image)
        assert imageMaterial != null

        // get and store the HTML page source of the page
        String html = driver.getPageSource()
        Material htmlMaterial = store.write(jobName, jobTimestamp, FileType.HTML, metadata,
                html, StandardCharsets.UTF_8)
        assert htmlMaterial != null

        return new Tuple(imageMaterial, htmlMaterial)
    }

    static void main(String[] args) {
        try {
            VisualTestingTwins instance = new VisualTestingTwins()
            instance.execute()
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
