package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.WithNull;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import testsupport.ExpectedLogs;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JobRunnerRemoveImageTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private JobRunner jobRunner;
    private String secretsVolumeName;
    private String otherTag;

    @BeforeContainer
    static void beforeContainer() {
        dockerClient = DockerClientFactory.createDefaultClient();
    }

    @AfterContainer
    static void afterContainer() {
        try {
            dockerClient.removeImageCmd(TEST_IMAGE).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @BeforeTry
    void setUp() throws InterruptedException {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
        jobRunner = new JobRunner(
                dockerAdapter,
                new ImageRemover(dockerAdapter, new ReentrantReadWriteLock().writeLock()),
                Runnable::run);
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
        TestImageContainers.removeAll(dockerClient, TEST_IMAGE);
    }

    @AfterTry
    void tearDown() {
        TestImageContainers.removeAll(dockerClient, TEST_IMAGE);
        try {
            if (secretsVolumeName != null && !secretsVolumeName.isBlank())
                dockerClient.removeVolumeCmd(secretsVolumeName).exec();
        } catch (Exception ignored) {}
        try {
            if (otherTag != null)
                dockerClient.removeImageCmd(otherTag).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @Property(tries = 3)
    @Label("Given a completed job run, " +
            "when removing the image after the run, " +
            "should be fully removed (not dangling).")
    void removeImageAfterRun(
            @ForAll("existingJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") @WithNull String secretsVolumeName,
            @ForAll Boolean useDockerCache
    ) throws CreateJobRunException {
        this.secretsVolumeName = secretsVolumeName;
        JobRunConfig jobRunConfig =
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache);

        String imageId = dockerClient.inspectImageCmd(jobName).exec().getId();
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        jobRunner.runContainer(jobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(imageId).exec());
    }

    @Property(tries = 3)
    @Label("Given a completed job run, " +
            "when not removing the image after the run, " +
            "should still exist after the run.")
    void dontRemoveImageAfterRun(
            @ForAll("existingJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") @WithNull String secretsVolumeName,
            @ForAll Boolean useDockerCache
    ) throws CreateJobRunException {
        this.secretsVolumeName = secretsVolumeName;
        JobRunConfig jobRunConfig =
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache);

        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        jobRunner.runContainer(jobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, false, false);

        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(jobName).exec());
    }

    @Example
    @ExpectedLogs(ImageRemover.class)
    @Label("Given an image with another tag, " +
            "when a job removes its image after its run, " +
            "should remove only the job's tag, " +
            "and keep the image and its other tag.")
    void otherTagKept() throws CreateJobRunException {
        String imageId = dockerClient.inspectImageCmd(TEST_IMAGE).exec().getId();
        String otherRepository = "conjob-test-other-tag-" + UUID.randomUUID();
        dockerClient.tagImageCmd(imageId, otherRepository, "latest").exec();
        otherTag = otherRepository + ":latest";

        String jobRunId = dockerAdapter.createJobRun(jobRunConfig(TEST_IMAGE));
        jobRunner.runContainer(jobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(TEST_IMAGE).exec());
        assertThat(dockerClient.inspectImageCmd(otherTag).exec().getId(), is(imageId));
    }

    @Example
    @ExpectedLogs(ImageRemover.class)
    @Label("Given a container kept from an earlier run of an image, " +
            "when a job removes the same image after its run, " +
            "should keep the image.")
    void imageOfKeptContainerKept() throws CreateJobRunException {
        String imageId = dockerClient.inspectImageCmd(TEST_IMAGE).exec().getId();
        String keptJobRunId = dockerAdapter.createJobRun(jobRunConfig(TEST_IMAGE));
        jobRunner.runContainer(keptJobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, false, false);

        String jobRunId = dockerAdapter.createJobRun(jobRunConfig(TEST_IMAGE));
        jobRunner.runContainer(jobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(imageId).exec());
        assertDoesNotThrow(() -> dockerClient.inspectContainerCmd(keptJobRunId).exec());
    }

    private JobRunConfig jobRunConfig(String image) {
        return new JobRunConfig(image, null, "cache-" + UUID.randomUUID(), null, false);
    }

    @Provide
    Arbitrary<String> existingJob() {
        return Arbitraries.just(TEST_IMAGE);
    }

    @Provide
    Arbitrary<String> dockerCacheVolumeName() {
        Arbitrary<String> firstChar = Arbitraries.strings().alpha().numeric().ofLength(1);
        Arbitrary<String> lastChars = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(254);
        return Combinators.combine(firstChar, lastChars).as((first, last) -> first + last);
    }

    @Provide
    Arbitrary<String> secretsVolumeName() {
        Arbitrary<String> firstChar = Arbitraries.strings().alpha().numeric().ofLength(1);
        Arbitrary<String> lastChars = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(254);
        return Combinators.combine(firstChar, lastChars).as((first, last) -> first + last);
    }
}
