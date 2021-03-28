package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerExit;
import conjob.core.job.exception.StopJobRunException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterStopContainerTest {
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
            "and a timeout until kill, " +
            "when stopping that container, " +
            "should return an exit status code.")
    void stopContainerSuccessfully(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds,
            @ForAll long expectedCode
    ) throws DockerException, InterruptedException, StopJobRunException {
        ContainerExit mockContainerExit = mock(ContainerExit.class);
        when(mockClient.waitContainer(givenContainerId)).thenReturn(mockContainerExit);
        when(mockContainerExit.statusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds);

        assertThat(exitStatusCode, is(expectedCode));
        verify(mockClient).stopContainer(givenContainerId, givenKillTimeoutSeconds);
        verify(mockClient).waitContainer(givenContainerId);
    }

    @Property
    @Label("Given a container id, " +
            "when stopping that container, " +
            "and a DockerException is thrown, " +
            "should throw a StopJobRunException.")
    void stopContainerDockerException(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds
    ) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }

    @Property
    @Label("Given a container id, " +
            "when stopping that container, " +
            "and a InterruptedException is thrown, " +
            "should throw a StopJobRunException.")
    void startContainerInterruptedException(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds
    ) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }

    @Property
    @Label("Given a container id, " +
            "when stopping that container, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void startContainerUnexpectedException(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds
    ) throws DockerException, InterruptedException {
        doThrow(new RuntimeException("")).when(mockClient).waitContainer(givenContainerId);

        assertThrows(RuntimeException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }
}