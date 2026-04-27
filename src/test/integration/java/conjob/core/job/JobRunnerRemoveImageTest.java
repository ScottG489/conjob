package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.WithNull;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JobRunnerRemoveImageTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private JobRunner jobRunner;
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
        jobRunner = new JobRunner(dockerAdapter);
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
    }

    @AfterTry
    void tearDown() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withAncestorFilter(List.of(TEST_IMAGE))
                    .exec();
            for (Container container : containers) {
                try {
                    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try {
            if (secretsVolumeName != null && !secretsVolumeName.isBlank())
                dockerClient.removeVolumeCmd(secretsVolumeName).exec();
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
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, true);

        String imageId = dockerClient.inspectImageCmd(jobName).exec().getId();
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        jobRunner.runContainer(jobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, jobName, true);

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
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, false);

        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        jobRunner.runContainer(jobRunId, Long.MAX_VALUE, Integer.MAX_VALUE, jobName, false);

        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(jobName).exec());
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
