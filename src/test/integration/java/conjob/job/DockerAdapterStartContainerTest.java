package conjob.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.RunJobException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterStartContainerTest {
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
    @Label("Given a created container, " +
            "when starting that container, " +
            "and it exits, " +
            "should return the code it exited with.")
    void startContainerThenWaitForExit(@ForAll @LongRange(max = 255) long givenExitCode)
            throws RunJobException, DockerException, InterruptedException {
        containerId = dockerClient.createContainer(ContainerConfig.builder()
                .image(TEST_SUPPORT_CONTAINER).cmd("0|||" + givenExitCode).build())
                .id();
        Long exitStatusCode = dockerAdapter.startContainerThenWaitForExit(containerId);

        assertThat(exitStatusCode, is(givenExitCode));
    }

    @Property(tries = 100)
    @Label("Given a nonexistent container id, " +
            "when starting that container, " +
            "should throw a RunJobException.")
    void startContainerDockerException(@ForAll("wellFormedContainerId") String givenContainerId) {
        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Provide
    Arbitrary<String> wellFormedContainerId() {
        return Arbitraries.strings().withCharRange('a', 'f').numeric().ofLength(64);
    }
}
