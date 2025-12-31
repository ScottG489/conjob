package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
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

    private RemoveContainerCmd setupRemoveContainerMock(String containerName) {
        RemoveContainerCmd mockCmd = mock(RemoveContainerCmd.class);
        when(mockClient.removeContainerCmd(containerName)).thenReturn(mockCmd);
        when(mockCmd.withForce(true)).thenReturn(mockCmd);
        when(mockCmd.withRemoveVolumes(true)).thenReturn(mockCmd);
        return mockCmd;
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "should finish successfully.")
    void removeContainerSuccessfully(@ForAll String containerName)  {
        RemoveContainerCmd mockCmd = setupRemoveContainerMock(containerName);

        dockerAdapter.removeContainer(containerName);

        verify(mockCmd).exec();
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "and an Exception is thrown, " +
            "should throw a RemoveContainerException.")
    void removeContainerException(@ForAll String containerName)  {
        RemoveContainerCmd mockCmd = setupRemoveContainerMock(containerName);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(RemoveContainerException.class, () -> dockerAdapter.removeContainer(containerName));
    }

    @Property
    @Label("Given a container name, " +
            "when removing that container, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void removeContainerUnexpectedException(@ForAll String containerName)  {
        RemoveContainerCmd mockCmd = setupRemoveContainerMock(containerName);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(RuntimeException.class, () -> dockerAdapter.removeContainer(containerName));
    }
}