package conjob.core.secrets;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.secrets.exception.RemoveSecretsContainerException;
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

    private RemoveContainerCmd setupRemoveContainerMock(String containerId) {
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class);
        when(mockClient.removeContainerCmd(containerId)).thenReturn(mockCmd);
        return mockCmd;
    }

    @Property
    @Label("Given a container id, " +
            "when removing that container, " +
            "should finish successfully.")
    void removeContainerSuccessfully(@ForAll String containerId) throws JobUpdateException {
        RemoveContainerCmd mockCmd = setupRemoveContainerMock(containerId);

        secretsAdapter.removeContainer(containerId);

        verify(mockCmd).exec();
    }

    @Property
    @Label("Given a container id, " +
            "when removing that container, " +
            "and an Exception is thrown, " +
            "should throw a RemoveSecretsContainerException.")
    void removeContainerException(@ForAll String containerId)  {
        RemoveContainerCmd mockCmd = setupRemoveContainerMock(containerId);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(RemoveSecretsContainerException.class, () -> secretsAdapter.removeContainer(containerId));
    }

    @Property
    @Label("Given a container id, " +
            "when removing that container, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void removeContainerUnexpectedException(@ForAll String containerId)  {
        RemoveContainerCmd mockCmd = setupRemoveContainerMock(containerId);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(RuntimeException.class, () -> secretsAdapter.removeContainer(containerId));
    }
}
