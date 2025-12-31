package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import conjob.core.job.exception.ReadLogsException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterReadAllLogsTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;
    private LogContainerCmd mockLogCmd;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        mockLogCmd = mock(LogContainerCmd.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "should return all of the log contents.")
    void readLogsSuccessfully(
            @ForAll String givenContainerId,
            @ForAll String expectedLogs
    ) throws Exception {
        when(mockClient.logContainerCmd(givenContainerId)).thenReturn(mockLogCmd);
        when(mockLogCmd.withStdOut(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.withStdErr(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.withFollowStream(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.exec(any(LogContainerResultCallback.class))).thenAnswer(invocation -> {
            LogContainerResultCallback callback = invocation.getArgument(0);

            // Simulate Docker sending log data by calling onNext with a Frame
            Frame frame = new Frame(StreamType.STDOUT, expectedLogs.getBytes());
            callback.onNext(frame);

            // Spy to make awaitCompletion non-blocking
            LogContainerResultCallback spiedCallback = spy(callback);
            doReturn(spiedCallback).when(spiedCallback).awaitCompletion();
            return spiedCallback;
        });

        String actualLogs = dockerAdapter.readAllLogsUntilExit(givenContainerId);

        assertThat(actualLogs, is(expectedLogs));
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "and an Exception is thrown, " +
            "should throw a ReadLogsException.")
    void readLogsDockerException(@ForAll String givenContainerId) throws Exception {
        when(mockClient.logContainerCmd(givenContainerId)).thenReturn(mockLogCmd);
        when(mockLogCmd.withStdOut(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.withStdErr(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.withFollowStream(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.exec(any(LogContainerResultCallback.class))).thenThrow(new RuntimeException(""));

        assertThrows(ReadLogsException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void readLogsUnexpectedException(@ForAll String givenContainerId) throws Exception {
        when(mockClient.logContainerCmd(givenContainerId)).thenReturn(mockLogCmd);
        when(mockLogCmd.withStdOut(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.withStdErr(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.withFollowStream(true)).thenReturn(mockLogCmd);
        when(mockLogCmd.exec(any(LogContainerResultCallback.class))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }
}
