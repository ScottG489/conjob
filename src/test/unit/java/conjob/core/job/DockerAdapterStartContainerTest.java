package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerExit;
import conjob.core.job.exception.RunJobException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterStartContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Test
    @DisplayName("Given a container ID," +
            "when starting that container," +
            "should return an exit status code")
    void startContainerSuccessfully() throws DockerException, InterruptedException, RunJobException {
        String givenContainerId = "container_id";
        Long expectedCode = 0L;

        ContainerExit mockContainerExit = mock(ContainerExit.class);
        when(mockClient.waitContainer(givenContainerId)).thenReturn(mockContainerExit);
        when(mockContainerExit.statusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.startContainerThenWaitForExit(givenContainerId);

        assertThat(exitStatusCode, is(expectedCode));
        verify(mockClient).startContainer(givenContainerId);
        verify(mockClient).waitContainer(givenContainerId);
    }

    @Test
    @DisplayName("Given a container id," +
            "when starting that container," +
            "and a DockerException is thrown," +
            "should throw a RunJobException")
    void startContainerDockerException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";
        doThrow(new DockerException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Test
    @DisplayName("Given a container id," +
            "when starting that container," +
            "and a InterruptedException is thrown," +
            "should throw a RunJobException")
    void startContainerInterruptedExceptionException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";
        doThrow(new InterruptedException()).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Test
    @DisplayName("Given a container id," +
            "when starting that container," +
            "and a unexpected Exception is thrown," +
            "should throw that exception")
    void startContainerUnexpectedExceptionException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";
        doThrow(new RuntimeException()).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RuntimeException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }
}