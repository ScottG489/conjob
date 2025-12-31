package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.StopJobRunException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterStopContainerTest {
    private static final String TEST_SUPPORT_CONTAINER = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;
    private DockerAdapter dockerAdapter;
    private String containerId;

    @BeforeContainer
    static void beforeAll() throws InterruptedException {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.pullImageCmd(TEST_SUPPORT_CONTAINER).start().awaitCompletion();
    }

    @AfterContainer
    static void afterAll()   {
        dockerClient.removeImageCmd(TEST_SUPPORT_CONTAINER).withForce(true).exec();
    }

    @BeforeTry
    void beforeEach() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @AfterTry
    void afterEach()   {
        if (containerId != null && !containerId.isBlank()) dockerClient.removeContainerCmd(containerId).exec();
    }

    @Property(tries = 10)
    @Label("Given a started container, " +
            "and it finishes, " +
            "when stopping that container, " +
            "should return the exit code it finished with.")
    void stopFinishedContainer(
            @ForAll @LongRange(max = 255) long givenExitCode
    ) throws StopJobRunException {
        int killTimeout = 999;
        containerId = dockerClient.createContainerCmd(TEST_SUPPORT_CONTAINER).withCmd("0|||" + givenExitCode).exec().getId();
        dockerClient.startContainerCmd(containerId).exec();
        dockerClient.waitContainerCmd(containerId).exec(new com.github.dockerjava.api.command.WaitContainerResultCallback()).awaitStatusCode();

        Long exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeout);

        assertThat(exitStatusCode, is(givenExitCode));
    }

    @Property(tries = 10)
    @Label("Given a started container, " +
            "and it finishes, " +
            "when killing that container, " +
            "should return the exit code it finished with.")
    void killFinishedContainer(
            @ForAll @LongRange(max = 255) long givenExitCode
    ) throws StopJobRunException {
        int killTimeout = 0;
        containerId = dockerClient.createContainerCmd(TEST_SUPPORT_CONTAINER).withCmd("0|||" + givenExitCode).exec().getId();
        dockerClient.startContainerCmd(containerId).exec();
        int foo = dockerClient.waitContainerCmd(containerId).exec(new com.github.dockerjava.api.command.WaitContainerResultCallback()).awaitStatusCode();

        Long exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeout);

        assertThat(exitStatusCode, is(givenExitCode));
    }

    @Property(tries = 10)
    @Label("Given a started container, " +
            "when stopping that container, " +
            "and it finishes before the kill timeout, " +
            "should return the exit code it finished with.")
    void stoppedButFinishedBeforeKill(
            @ForAll @LongRange(max = 255) long givenExitCode
    ) throws StopJobRunException {
        double containerRunDuration = .5;
        int killTimeout = (int) (containerRunDuration + 1);
        containerId = dockerClient.createContainerCmd(TEST_SUPPORT_CONTAINER).withCmd(containerRunDuration + "|||" + givenExitCode).exec().getId();
        dockerClient.startContainerCmd(containerId).exec();

        Long exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeout);

        assertThat(exitStatusCode, is(givenExitCode));
    }

    @Property(tries = 10)
    @Label("Given a started container, " +
            "when stopping that container, " +
            "and it's killed before it finishes, " +
            "should return the SIGTERM exit code.")
    void startedButKilledBeforeFinish(
            @ForAll @LongRange(max = 255) long givenExitCode
    ) throws StopJobRunException {
        int killTimeout = 0;
        double containerRunDuration = killTimeout + 1;
        containerId = dockerClient.createContainerCmd(TEST_SUPPORT_CONTAINER).withCmd(containerRunDuration + "|||" + givenExitCode).exec().getId();
        dockerClient.startContainerCmd(containerId).exec();

        Long exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeout);

        assertThat(exitStatusCode, is(137L));
    }

    @Property(tries = 100)
    @Label("Given a nonexistent container id, " +
            "when stopping that container, " +
            "should throw a StopJobRunException.")
    void stopContainerStopJobRunException(
            @ForAll("wellFormedContainerId") String givenContainerId,
            @ForAll int givenKillTimeoutSeconds
    ) {
        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }

    @Provide
    Arbitrary<String> wellFormedContainerId() {
        return Arbitraries.strings().withCharRange('a', 'f').numeric().ofLength(64);
    }
}
