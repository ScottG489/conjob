package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
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
    static void beforeAll() throws InterruptedException {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.pullImageCmd("tianon/true:latest").start().awaitCompletion();
    }

    @BeforeEach
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @Test
    @DisplayName("Given a container that exists, " +
            "when removing that container, " +
            "should finish successfully.")
    void removeContainerSuccessfully()   {
        String containerId = dockerClient.createContainerCmd("tianon/true").withCmd("").exec().getId();
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
