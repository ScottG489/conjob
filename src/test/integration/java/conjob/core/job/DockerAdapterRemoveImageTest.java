package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.RemoveImageException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.WithNull;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterRemoveImageTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private String secretsVolumeName;

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
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
    }

    @AfterTry
    void tearDown() {
        try {
            if (secretsVolumeName != null && !secretsVolumeName.isBlank())
                dockerClient.removeVolumeCmd(secretsVolumeName).exec();
        } catch (Exception ignored) {}
    }

    @Property(tries = 3)
    @Label("Given a completed job run, " +
            "when removing the image, " +
            "should not exist after removal.")
    void removeImageAfterRun(
            @ForAll("existingJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") @WithNull String secretsVolumeName,
            @ForAll Boolean useDockerCache
    ) throws CreateJobRunException {
        this.secretsVolumeName = secretsVolumeName;
        JobRunConfig jobRunConfig =
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, true);

        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        dockerAdapter.startContainerThenWaitForExit(jobRunId);

        assertDoesNotThrow(() -> dockerAdapter.removeImage(jobName));
    }

    @Property(tries = 3)
    @Label("Given a completed job run, " +
            "when not removing the image, " +
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
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, true);

        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        dockerAdapter.startContainerThenWaitForExit(jobRunId);

        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(jobName).exec());
    }

    @Property(tries = 3)
    @Label("Given an image that doesn't exist, " +
            "when removing that image, " +
            "should throw a RemoveImageException.")
    void removeImageNonexistent(@ForAll("nonexistentImage") String imageName) {
        assertThrows(RemoveImageException.class, () -> dockerAdapter.removeImage(imageName));
    }

    @Provide
    Arbitrary<String> existingJob() {
        return Arbitraries.just(TEST_IMAGE);
    }

    @Provide
    Arbitrary<String> nonexistentImage() {
        return Arbitraries.just("local/image-that-does-not-exist-bbd10827:latest");
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
