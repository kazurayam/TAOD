package com.kazurayam.materials.store

import com.kazurayam.materials.diff.DefaultDiffer
import com.kazurayam.materials.diff.DefaultReporter
import com.kazurayam.materials.diff.DiffArtifact
import com.kazurayam.materials.diff.Differ
import com.kazurayam.materials.diff.Reporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

class Store implements IStore {

    private static final Logger logger = LoggerFactory.getLogger(Store.class)

    private final Path root_

    private final Set<Jobber> jobberCache_

    private static final int BUFFER_SIZE = 8000

    private static final String ROOT_DIRECTORY_NAME = "Materials"

    Store() {
        this(Paths.get(ROOT_DIRECTORY_NAME))
    }

    Store(Path root) {
        Objects.requireNonNull(root)
        Files.createDirectories(root)
        this.root_ = root
        this.jobberCache_ = new HashSet<Jobber>()
    }

    Path getRoot() {
        return root_
    }


    @Override
    Material write(JobName jobName, JobTimestamp jobTimestamp,
                 Metadata meta, File input, FileType fileType) {
        Objects.requireNonNull(input)
        assert input.exists()
        FileInputStream fis = new FileInputStream(input)
        byte[] data = toByteArray(fis)
        fis.close()
        return this.write(jobName, jobTimestamp, meta, data, fileType)
    }

    @Override
    Material write(JobName jobName, JobTimestamp jobTimestamp,
                 Metadata meta, Path input, FileType fileType) {
        Objects.requireNonNull(input)
        assert Files.exists(input)
        FileInputStream fis = new FileInputStream(input.toFile())
        byte[] data = toByteArray(fis)
        fis.close()
        return this.write(jobName, jobTimestamp, meta, data, fileType)
    }


    private static byte[] toByteArray(InputStream inputStream) {
        Objects.requireNonNull(inputStream)
        byte[] buff = new byte[BUFFER_SIZE]
        int bytesRead
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        while ((bytesRead = inputStream.read(buff)) != -1) {
            baos.write(buff, 0, bytesRead)
        }
        inputStream.close()
        return baos.toByteArray()
    }


    @Override
    Material write(JobName jobName, JobTimestamp jobTimestamp,
             Metadata meta, BufferedImage input, FileType fileType) {
        Objects.requireNonNull(input)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(input, fileType.extension, baos);
        byte[] data = baos.toByteArray()
        baos.close()
        return this.write(jobName, jobTimestamp, meta, data, fileType)
    }


    @Override
    Material write(JobName jobName, JobTimestamp jobTimestamp,
                   Metadata meta, String input, FileType fileType,
                   String charsetName = "UTF-8") {
        Objects.requireNonNull(input)
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        Writer wrt = new BufferedWriter(
                new OutputStreamWriter(baos, charsetName))
        wrt.write(input)
        wrt.flush()
        byte[] data = baos.toByteArray()
        wrt.close()
        return this.write(jobName, jobTimestamp, meta, data, fileType)
    }

    @Override
    Material write(JobName jobName, JobTimestamp jobTimestamp,
                 Metadata meta, byte[] input, FileType fileType) {
        Objects.requireNonNull(root_)
        Objects.requireNonNull(jobName)
        Objects.requireNonNull(jobTimestamp)
        Objects.requireNonNull(meta)
        Objects.requireNonNull(fileType)
        Jobber jobber = this.getJobber(jobName, jobTimestamp)
        return jobber.commit(meta, input, fileType)
    }


    @Override
    Differ newDiffer(JobName jobName, JobTimestamp jobTimestamp) {
        return new DefaultDiffer(this.getRoot(), jobName, jobTimestamp)
    }

    @Override
    Reporter newReporter(JobName jobName, JobTimestamp jobTimestamp) {
        return new DefaultReporter(this.getRoot(), jobName, jobTimestamp)
    }


    @Override
    List<Material> select(JobName jobName, JobTimestamp jobTimestamp,
                          FileType fileType, MetadataPattern metadataPattern) {
        throw new UnsupportedOperationException("TODO")
    }


    /**
     * return an instance of Job.
     * if cached, return the found.
     * if not cached, return the new one.
     *
     * @param jobName
     * @param jobTimestamp
     * @return
     */
    Jobber getJobber(JobName jobName, JobTimestamp jobTimestamp) {
        Jobber jobber = getCachedJobber(jobName, jobTimestamp)
        if (jobber != null) {
            return jobber
        } else {
            Jobber newJob = new Jobber(root_, jobName, jobTimestamp)
            // put the new Job object in the cache
            jobberCache_.add(newJob)
            return newJob
        }
    }


    Jobber getCachedJobber(JobName jobName, JobTimestamp jobTimestamp) {
        Jobber result = null
        for (int i = 0; jobberCache_.size(); i++) {
            Jobber cached = jobberCache_[i]
            if (cached.getJobName() == jobName &&
                        cached.getJobTimestamp() == jobTimestamp) {
                result = cached
                break
            }
        }
        return result
    }

    /**
     *
     * @param jobName
     * @return null if directory of the jobName does not exists
     */
    List<Jobber> findJobbersOf(JobName jobName) {
        Path jobNamePath = root_.resolve(jobName.toString())
        if (! Files.exists(jobNamePath)) {
            return null
        }
        List<Jobber> result = Files.list(jobNamePath)
                .filter { Path p -> JobTimestamp.isValid(p.getFileName().toString() ) }
                .map { Path p -> new JobTimestamp(p.getFileName().toString()) }
                .map { JobTimestamp jt -> new Jobber(root_, jobName, jt) }
                .collect(Collectors.toList())
        return result
    }

    @Override
    List<DiffArtifact> zipMaterialsToDiff(JobName jobName, JobTimestamp jobTimestamp,
                                          MetadataPattern pattern1, MetadataPattern pattern2) {
        throw new UnsupportedOperationException("TODO")
    }
}
