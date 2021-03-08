package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerExit;
import conjob.core.job.exception.StopJobRunException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterStopContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Test
    @DisplayName("Given a container ID," +
            "and a timeout until kill," +
            "when stopping that container," +
            "should return an exit status code")
    void stopContainerSuccessfully() throws DockerException, InterruptedException, StopJobRunException {
        String givenContainerId = "container_id";
        int givenKillTimeoutSeconds = 1;
        Long expectedCode = 0L;

        ContainerExit mockContainerExit = mock(ContainerExit.class);
        when(mockClient.waitContainer(givenContainerId)).thenReturn(mockContainerExit);
        when(mockContainerExit.statusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds);

        assertThat(exitStatusCode, is(expectedCode));
        verify(mockClient).stopContainer(givenContainerId, givenKillTimeoutSeconds);
        verify(mockClient).waitContainer(givenContainerId);
    }

    @Test
    @DisplayName("Given a container id," +
            "when stopping that container," +
            "and a DockerException is thrown," +
            "should throw a StopJobRunException")
    void stopContainerDockerException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";
        int givenKillTimeoutSeconds = 1;

        doThrow(new DockerException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }

    @Test
    @DisplayName("Given a container id," +
            "when stopping that container," +
            "and a InterruptedException is thrown," +
            "should throw a StopJobRunException")
    void startContainerInterruptedException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";
        int givenKillTimeoutSeconds = 1;

        doThrow(new DockerException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }

    @Test
    @DisplayName("Given a container id," +
            "when stopping that container," +
            "and an unexpected Exception is thrown," +
            "should throw that exception")
    void startContainerUnexpectedException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";
        int givenKillTimeoutSeconds = 1;

        doThrow(new RuntimeException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RuntimeException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }
}