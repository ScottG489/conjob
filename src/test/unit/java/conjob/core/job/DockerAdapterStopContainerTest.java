package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotModifiedException;
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
    private StopContainerCmd mockStopCmd;
    private WaitContainerCmd mockWaitCmd;
    private WaitContainerResultCallback mockCallback;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        mockStopCmd = mock(StopContainerCmd.class);
        mockWaitCmd = mock(WaitContainerCmd.class);
        mockCallback = mock(WaitContainerResultCallback.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    private void setupStopContainerMock(String containerId, int timeoutSeconds) {
        when(mockClient.stopContainerCmd(containerId)).thenReturn(mockStopCmd);
        when(mockStopCmd.withTimeout(timeoutSeconds)).thenReturn(mockStopCmd);
    }

    private void setupWaitForExitMock(String containerId) {
        when(mockClient.waitContainerCmd(containerId)).thenReturn(mockWaitCmd);
        when(mockWaitCmd.exec(any(WaitContainerResultCallback.class))).thenReturn(mockCallback);
    }

    @Property
    @Label("Given a container ID, " +
            "and a timeout until kill, " +
            "when stopping that container, " +
            "should return an exit status code.")
    void stopContainerSuccessfully(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds,
            @ForAll int expectedCode
    ) throws StopJobRunException {
        setupStopContainerMock(givenContainerId, givenKillTimeoutSeconds);
        setupWaitForExitMock(givenContainerId);
        when(mockCallback.awaitStatusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds);

        assertThat(exitStatusCode, is((long) expectedCode));
        verify(mockStopCmd).exec();
        verify(mockCallback).awaitStatusCode();
    }

    @Property
    @Label("Given a container id, " +
            "when stopping that container, " +
            "and a NotModifiedException is thrown, " +
            "should return an exit status code.")
    void startContainerNotModifiedException(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds,
            @ForAll int expectedCode
    )  {
        setupStopContainerMock(givenContainerId, givenKillTimeoutSeconds);
        doThrow(new NotModifiedException("")).when(mockStopCmd).exec();
        setupWaitForExitMock(givenContainerId);
        when(mockCallback.awaitStatusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds);

        assertThat(exitStatusCode, is((long) expectedCode));
    }

    @Property
    @Label("Given a container id, " +
            "when stopping that container, " +
            "and an Exception is thrown, " +
            "should throw a StopJobRunException.")
    void stopContainerDockerException(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds
    )  {
        setupStopContainerMock(givenContainerId, givenKillTimeoutSeconds);
        doThrow(new RuntimeException("")).when(mockStopCmd).exec();

        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }

    @Property
    @Label("Given a container id, " +
            "when stopping that container, " +
            "and an exception is thrown waiting for the status code, " +
            "should throw a StopJobRunException.")
    void startContainerUnexpectedException(
            @ForAll String givenContainerId,
            @ForAll int givenKillTimeoutSeconds
    )  {
        setupStopContainerMock(givenContainerId, givenKillTimeoutSeconds);
        setupWaitForExitMock(givenContainerId);
        when(mockCallback.awaitStatusCode()).thenThrow(new RuntimeException(""));

        assertThrows(StopJobRunException.class, () -> dockerAdapter.stopContainer(givenContainerId, givenKillTimeoutSeconds));
    }
}
