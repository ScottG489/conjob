package conjob.core.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import conjob.core.job.exception.RemoveContainerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterRemoveContainerTest {
    private static final String NONEXISTENT_CONTAINER_ID = "a24db8027935404b96c1beff8742e2e14292bd5e6aa544d5811173f8ce25ba17";
    private static DockerClient dockerClient;
    private DockerAdapter dockerAdapter;

    @BeforeAll
    static void beforeAll() throws DockerCertificateException {
        dockerClient = DefaultDockerClient.fromEnv().build();
    }

    @BeforeEach
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @Test
    @DisplayName("Given a container that exists, " +
            "when removing that container, " +
            "should finish successfully.")
    void removeContainerSuccessfully() throws DockerException, InterruptedException {
        String containerId = dockerClient.createContainer(ContainerConfig.builder().cmd("").build()).id();
        dockerAdapter.removeContainer(containerId);
    }

    @Test
    @DisplayName("Given a container that doesn't exist, " +
            "when removing that container, " +
            "should throw a RemoveContainerException.")
    void RemoveContainerException() {
        assertThrows(RemoveContainerException.class, () -> dockerAdapter.removeContainer(NONEXISTENT_CONTAINER_ID));
    }
}
