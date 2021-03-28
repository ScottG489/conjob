package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerExit;
import conjob.core.job.exception.RunJobException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterStartContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a container ID, " +
            "when starting that container, " +
            "should return an exit status code.")
    void startContainerSuccessfully(
            @ForAll String givenContainerId,
            @ForAll long expectedCode
    ) throws DockerException, InterruptedException, RunJobException {
        ContainerExit mockContainerExit = mock(ContainerExit.class);
        when(mockClient.waitContainer(givenContainerId)).thenReturn(mockContainerExit);
        when(mockContainerExit.statusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.startContainerThenWaitForExit(givenContainerId);

        assertThat(exitStatusCode, is(expectedCode));
        verify(mockClient).startContainer(givenContainerId);
        verify(mockClient).waitContainer(givenContainerId);
    }

    @Property
    @Label("Given a container id, " +
            "when starting that container, " +
            "and a DockerException is thrown, " +
            "should throw a RunJobException.")
    void startContainerDockerException(@ForAll String givenContainerId) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Property
    @Label("Given a container id, " +
            "when starting that container, " +
            "and a InterruptedException is thrown, " +
            "should throw a RunJobException.")
    void startContainerInterruptedExceptionException(@ForAll String givenContainerId) throws DockerException, InterruptedException {
        doThrow(new InterruptedException()).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Property
    @Label("Given a container id, " +
            "when starting that container, " +
            "and a unexpected Exception is thrown, " +
            "should throw that exception.")
    void startContainerUnexpectedExceptionException(@ForAll String givenContainerId) throws DockerException, InterruptedException {
        doThrow(new RuntimeException()).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RuntimeException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }
}