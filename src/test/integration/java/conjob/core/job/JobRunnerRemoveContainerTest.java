package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import conjob.core.job.model.JobRunOutcome;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import testsupport.ExpectedLogs;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JobRunnerRemoveContainerTest {
    private static final String ECHO_IMAGE = "scottg489/echo-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private JobRunner jobRunner;
    private String containerId;

    @BeforeContainer
    static void beforeContainer() throws InterruptedException {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.pullImageCmd(ECHO_IMAGE).start().awaitCompletion();
    }

    @AfterContainer
    static void afterContainer() {
        try {
            dockerClient.removeImageCmd(ECHO_IMAGE).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @BeforeTry
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
        jobRunner = new JobRunner(dockerAdapter);
    }

    @AfterTry
    void tearDown() {
        try {
            if (containerId != null)
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @Property(tries = 100)
    @Label("Given a job that exits right away, " +
            "and remove is true, " +
            "when running the job, " +
            "should return its exit code and output, " +
            "and its container should not exist after.")
    void quickJobRemoved(@ForAll("echoInput") String givenInput) throws CreateJobRunException {
        containerId = createJobRun(givenInput);

        JobRunOutcome outcome = jobRunner.runContainer(containerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, false);

        assertThat(outcome.getExitStatusCode(), is(0L));
        assertThat(outcome.getOutput(), is(givenInput));
        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(containerId).exec());
    }

    @Property(tries = 10)
    @Label("Given remove is false, " +
            "when running a job, " +
            "its container should still exist after.")
    void containerKept(@ForAll("echoInput") String givenInput) throws CreateJobRunException {
        containerId = createJobRun(givenInput);

        jobRunner.runContainer(containerId, Long.MAX_VALUE, Integer.MAX_VALUE, false, false);

        assertDoesNotThrow(() -> dockerClient.inspectContainerCmd(containerId).exec());
    }

    @Example
    @ExpectedLogs(JobRunner.class)
    @Label("Given a container that fails to start, " +
            "and remove is true, " +
            "when running it, " +
            "should return an exit code of -1, " +
            "and the container should not exist after.")
    void failedStartRemoved() {
        containerId = dockerClient.createContainerCmd(ECHO_IMAGE)
                .withEntrypoint("/entrypoint-that-does-not-exist")
                .exec().getId();

        JobRunOutcome outcome = jobRunner.runContainer(containerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, false);

        assertThat(outcome.getExitStatusCode(), is(-1L));
        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(containerId).exec());
    }

    private String createJobRun(String input) throws CreateJobRunException {
        return dockerAdapter.createJobRun(
                new JobRunConfig(ECHO_IMAGE, input, "cache-" + UUID.randomUUID(), null, false));
    }

    @Provide
    Arbitrary<String> echoInput() {
        return Arbitraries.strings().ascii().excludeChars('\0').ofMinLength(1);
    }
}
