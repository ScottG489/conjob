package conjob.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
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
    private static DefaultDockerClient dockerClient;
    private DockerAdapter dockerAdapter;
    private String containerId;

    @BeforeContainer
    static void beforeAll() throws DockerCertificateException, DockerException, InterruptedException {
        dockerClient = DefaultDockerClient.fromEnv().build();
        dockerClient.pull(TEST_SUPPORT_CONTAINER);
    }

    @AfterContainer
    static void afterAll() throws DockerException, InterruptedException {
        dockerClient.removeImage(TEST_SUPPORT_CONTAINER, true, false);
    }

    @BeforeTry
    void beforeEach() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @AfterTry
    void afterEach() throws DockerException, InterruptedException {
        if (containerId != null && !containerId.isBlank()) dockerClient.removeContainer(containerId);
    }

    @Property(tries = 10)
    @Label("Given a started container, " +
            "and it finishes, " +
            "when stopping that container, " +
            "should return the exit code it finished with.")
    void stopFinishedContainer(
            @ForAll @LongRange(max = 255) long givenExitCode
    ) throws StopJobRunException, DockerException, InterruptedException {
        int killTimeout = 999;
        containerId = dockerClient.createContainer(ContainerConfig.builder()
                .image(TEST_SUPPORT_CONTAINER).cmd("0||" + givenExitCode).build())
                .id();
        dockerClient.startContainer(containerId);
        dockerClient.waitContainer(containerId);

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
    ) throws StopJobRunException, DockerException, InterruptedException {
        int killTimeout = 0;
        containerId = dockerClient.createContainer(ContainerConfig.builder()
                .image(TEST_SUPPORT_CONTAINER).cmd("0||" + givenExitCode).build())
                .id();
        dockerClient.startContainer(containerId);
        dockerClient.waitContainer(containerId);

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
    ) throws StopJobRunException, DockerException, InterruptedException {
        double containerRunDuration = .5;
        int killTimeout = (int) (containerRunDuration + 1);
        containerId = dockerClient.createContainer(ContainerConfig.builder()
                .image(TEST_SUPPORT_CONTAINER).cmd(containerRunDuration + "||" + givenExitCode).build())
                .id();
        dockerClient.startContainer(containerId);

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
    ) throws StopJobRunException, DockerException, InterruptedException {
        int killTimeout = 0;
        double containerRunDuration = killTimeout + 1;
        containerId = dockerClient.createContainer(ContainerConfig.builder()
                .image(TEST_SUPPORT_CONTAINER).cmd(containerRunDuration + "||" + givenExitCode).build())
                .id();
        dockerClient.startContainer(containerId);

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
