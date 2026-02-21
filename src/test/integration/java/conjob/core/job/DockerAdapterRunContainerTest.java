package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.WaitContainerCondition;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.WithNull;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterRunContainerTest {
    private static final String TEST_SUPPORT_CONTAINER = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private String jobRunId;
    private String secretsVolumeName;

    @BeforeContainer
    static void beforeContainer() throws InterruptedException {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.pullImageCmd(TEST_SUPPORT_CONTAINER).start().awaitCompletion();
    }

    @AfterContainer
    static void afterContainer()   {
        dockerClient.removeImageCmd(TEST_SUPPORT_CONTAINER).withForce(true).exec();
    }

    @BeforeTry
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
    }

    @AfterTry
    void tearDown()   {
        try {
            if (jobRunId != null && !jobRunId.isBlank()) dockerClient.removeContainerCmd(jobRunId).exec();
            if (secretsVolumeName != null && !secretsVolumeName.isBlank()) dockerClient.removeVolumeCmd(secretsVolumeName).exec();
        } catch (Exception e) {}
    }

    @Property(tries = 100)
    @Label("Given a JobRunConfig, " +
            "and it's specified to remove the container, " +
            "when running a job, " +
            "should not exist after finishing.")
    void createThenRemoveJobRun(
            @ForAll("existingJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") @WithNull String secretsVolumeName,
            @ForAll Boolean useDockerCache
    ) throws CreateJobRunException {
        this.secretsVolumeName = secretsVolumeName;
        JobRunConfig jobRunConfig =
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, true);

        jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        dockerAdapter.startContainerThenWaitForExit(jobRunId);

        try {
            dockerClient.waitContainerCmd(jobRunId)
                    .withCondition(WaitContainerCondition.REMOVED)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(jobRunId).exec());
    }

    @Property(tries = 100)
    @Label("Given a JobRunConfig, " +
            "and it's specified to not remove the container, " +
            "when running a job, " +
            "should exist after finishing.")
    void createThenDontRemoveJobRun(
            @ForAll("existingJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") @WithNull String secretsVolumeName,
            @ForAll Boolean useDockerCache
    ) {
        this.secretsVolumeName = secretsVolumeName;
        JobRunConfig jobRunConfig =
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, false);

        jobRunId = dockerAdapter.createJobRun(jobRunConfig);
        dockerAdapter.startContainerThenWaitForExit(jobRunId);

        assertDoesNotThrow(() -> dockerClient.inspectContainerCmd(jobRunId).exec());
    }

    @Provide
    Arbitrary<String> existingJob() {
        return Arbitraries.just(TEST_SUPPORT_CONTAINER);
    }

    @Provide
    Arbitrary<String> nonexistantJob() {
        return Arbitraries.just("local/job-that-does-not-exist-aeec82aa:latest");
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
