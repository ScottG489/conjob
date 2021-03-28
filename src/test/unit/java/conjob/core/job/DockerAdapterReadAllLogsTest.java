package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
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

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "should return all of the log contents.")
    void readLogsSuccessfully(
            @ForAll String givenContainerId,
            @ForAll String expectedLogs
    ) throws ReadLogsException, DockerException, InterruptedException {
        LogStream mockLogStream = mock(LogStream.class);

        when(mockClient.logs(eq(givenContainerId), any(DockerClient.LogsParam.class)))
                .thenReturn(mockLogStream);
        when(mockLogStream.readFully()).thenReturn(expectedLogs);

        String actualLogs = dockerAdapter.readAllLogsUntilExit(givenContainerId);

        assertThat(actualLogs, is(expectedLogs));
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "and a DockerException is thrown, " +
            "should throw a ReadLogsException.")
    void readLogsDockerException(@ForAll String givenContainerId) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient)
                .logs(eq(givenContainerId), any(DockerClient.LogsParam.class));

        assertThrows(ReadLogsException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "and a InterruptedException is thrown, " +
            "should throw a ReadLogsException.")
    void readLogsInterruptedException(@ForAll String givenContainerId) throws DockerException, InterruptedException {
        doThrow(new InterruptedException("")).when(mockClient)
                .logs(eq(givenContainerId), any(DockerClient.LogsParam.class));

        assertThrows(ReadLogsException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }

    @Property
    @Label("Given a container ID, " +
            "when reading that container's logs, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void readLogsUnexpectedException(@ForAll String givenContainerId) throws DockerException, InterruptedException {
        doThrow(new RuntimeException("")).when(mockClient)
                .logs(eq(givenContainerId), any(DockerClient.LogsParam.class));

        assertThrows(RuntimeException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }
}