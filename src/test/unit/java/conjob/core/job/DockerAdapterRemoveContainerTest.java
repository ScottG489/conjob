package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.exception.RemoveContainerException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterRemoveContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "should finish successfully.")
    void removeContainerSuccessfully(@ForAll String containerName) throws DockerException, InterruptedException {
        dockerAdapter.removeContainer(containerName);
        verify(mockClient).removeContainer(
                containerName,
                DockerClient.RemoveContainerParam.forceKill(),
                DockerClient.RemoveContainerParam.removeVolumes());
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "and a DockerException is thrown, " +
            "should throw a RemoveContainerException.")
    void removeContainerDockerException(@ForAll String containerName) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).removeContainer(
                containerName,
                DockerClient.RemoveContainerParam.forceKill(),
                DockerClient.RemoveContainerParam.removeVolumes());

        assertThrows(RemoveContainerException.class, () -> dockerAdapter.removeContainer(containerName));
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "and an InterruptedException is thrown, " +
            "should throw a RemoveContainerException.")
    void removeContainerInterruptedException(@ForAll String containerName) throws DockerException, InterruptedException {
        doThrow(new InterruptedException("")).when(mockClient).removeContainer(
                containerName,
                DockerClient.RemoveContainerParam.forceKill(),
                DockerClient.RemoveContainerParam.removeVolumes());

        assertThrows(RemoveContainerException.class, () -> dockerAdapter.removeContainer(containerName));
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void removeContainerUnexpectedException(@ForAll String containerName) throws DockerException, InterruptedException {
        doThrow(new RuntimeException("")).when(mockClient).removeContainer(
                containerName,
                DockerClient.RemoveContainerParam.forceKill(),
                DockerClient.RemoveContainerParam.removeVolumes());

        assertThrows(RuntimeException.class, () -> dockerAdapter.removeContainer(containerName));
    }
}