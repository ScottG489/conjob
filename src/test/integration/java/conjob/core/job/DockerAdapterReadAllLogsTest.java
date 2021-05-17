package conjob.core.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.ReadLogsException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterReadAllLogsTest {
    private static final String IMAGE_WITH_LOG_OUTPUT = "scottg489/echo-job";
    private DockerAdapter dockerAdapter;
    private static DefaultDockerClient dockerClient;

    private String containerId;

    @BeforeContainer
    static void beforeAll() throws DockerCertificateException, DockerException, InterruptedException {
        dockerClient = DefaultDockerClient.fromEnv().build();
        dockerClient.pull(IMAGE_WITH_LOG_OUTPUT);
    }

    @AfterContainer
    static void afterAll() throws DockerException, InterruptedException {
        dockerClient.removeImage(IMAGE_WITH_LOG_OUTPUT, true, false);
    }

    @BeforeTry
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @AfterTry
    void tearDown() throws DockerException, InterruptedException {
        if (containerId != null && !containerId.isBlank()) dockerClient.removeContainer(containerId);
    }

    // TODO: See TODO in DockerAdapter.readAllLogsUntilExit for why this is ignored
//    @Property(tries = 10)
    @Label("Given a container that outputs logs, " +
            "and that container is run, " +
            "when reading that containers logs, " +
            "should return all of the log contents.")
    void readLogsSuccessfully(@ForAll("validInputToEchoJob") String givenLogs)
            throws ReadLogsException, DockerException, InterruptedException {
        containerId = dockerClient.createContainer(ContainerConfig.builder()
                .image(IMAGE_WITH_LOG_OUTPUT)
                .cmd(givenLogs).build()).id();
        dockerClient.startContainer(containerId);

        String actualLogs = dockerAdapter.readAllLogsUntilExit(containerId);

        assertThat(actualLogs, is(givenLogs));
    }

    @Property(tries = 100)
    @Label("Given a container ID that doesn't exist, " +
            "when reading that container's logs, " +
            "should throw a ReadLogsException.")
    void readLogsDockerException(@ForAll("validContainerId") String givenContainerId) {
        assertThrows(ReadLogsException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }

    @Provide
    Arbitrary<String> validInputToEchoJob() {
        return Arbitraries.strings().ascii().excludeChars('\0');
    }

    @Provide
    Arbitrary<String> validContainerId() {
        return Arbitraries.strings().withCharRange('a', 'f').numeric().ofLength(64);
    }
}
