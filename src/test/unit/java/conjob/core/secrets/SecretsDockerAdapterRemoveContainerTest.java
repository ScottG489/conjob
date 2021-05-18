package conjob.core.secrets;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.secrets.exception.RemoveSecretsContainerException;
import conjob.core.secrets.exception.UpdateSecretsImageException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class SecretsDockerAdapterRemoveContainerTest {
    private SecretsDockerAdapter secretsAdapter;
    private DockerClient mockClient;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        secretsAdapter = new SecretsDockerAdapter(mockClient);
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "should finish successfully.")
    void pullImageSuccessfully(@ForAll String containerId) throws JobUpdateException, DockerException, InterruptedException {
        secretsAdapter.removeContainer(containerId);
        verify(mockClient).removeContainer(containerId);
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and a DockerException is thrown, " +
            "should throw a RemoveSecretsContainerException.")
    void pullImageDockerException(@ForAll String imageName) throws DockerException, InterruptedException {
        doThrow(DockerException.class).when(mockClient).removeContainer(imageName);

        assertThrows(RemoveSecretsContainerException.class, () -> secretsAdapter.removeContainer(imageName));
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an InterruptedException is thrown, " +
            "should throw a RemoveSecretsContainerException.")
    void pullImageInterruptedException(@ForAll String imageName) throws DockerException, InterruptedException {
        doThrow(InterruptedException.class).when(mockClient).removeContainer(imageName);

        assertThrows(RemoveSecretsContainerException.class, () -> secretsAdapter.removeContainer(imageName));
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void pullImageUnexpectedException(@ForAll String imageName) throws DockerException, InterruptedException {
        doThrow(RuntimeException.class).when(mockClient).removeContainer(imageName);

        assertThrows(RuntimeException.class, () -> secretsAdapter.removeContainer(imageName));
    }
}