package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
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
    private StartContainerCmd mockStartCmd;
    private WaitContainerCmd mockWaitCmd;
    private WaitContainerResultCallback mockCallback;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        mockStartCmd = mock(StartContainerCmd.class);
        mockWaitCmd = mock(WaitContainerCmd.class);
        mockCallback = mock(WaitContainerResultCallback.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    private void setupStartContainerMock(String containerId) {
        when(mockClient.startContainerCmd(containerId)).thenReturn(mockStartCmd);
        when(mockClient.waitContainerCmd(containerId)).thenReturn(mockWaitCmd);
        when(mockWaitCmd.exec(any(WaitContainerResultCallback.class))).thenReturn(mockCallback);
    }

    @Property
    @Label("Given a container ID, " +
            "when starting that container, " +
            "should return an exit status code.")
    void startContainerSuccessfully(
            @ForAll String givenContainerId,
            @ForAll int expectedCode
    ) throws RunJobException {
        setupStartContainerMock(givenContainerId);
        when(mockCallback.awaitStatusCode()).thenReturn(expectedCode);

        Long exitStatusCode = dockerAdapter.startContainerThenWaitForExit(givenContainerId);

        assertThat(exitStatusCode, is((long) expectedCode));
        verify(mockStartCmd).exec();
        verify(mockCallback).awaitStatusCode();
    }

    @Property
    @Label("Given a container id, " +
            "when starting that container, " +
            "and an Exception is thrown, " +
            "should throw a RunJobException.")
    void startContainerDockerException(@ForAll String givenContainerId)  {
        setupStartContainerMock(givenContainerId);
        when(mockCallback.awaitStatusCode()).thenThrow(new RuntimeException(""));

        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Property
    @Label("Given a container id, " +
            "when starting that container, " +
            "and a unexpected Exception is thrown, " +
            "should throw that exception.")
    void startContainerUnexpectedExceptionException(@ForAll String givenContainerId)  {
        setupStartContainerMock(givenContainerId);
        when(mockCallback.awaitStatusCode()).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }
}
