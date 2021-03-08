package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.exception.ReadLogsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterReadAllLogsTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Test
    @DisplayName("Given a container ID," +
            "when reading that container's logs," +
            "should return all of the log contents")
    void readLogsSuccessfully() throws ReadLogsException, DockerException, InterruptedException {
        String givenContainerId = "container_id";
        String expectedLogs = "some_logs";
        LogStream mockLogStream = mock(LogStream.class);

        when(mockClient.logs(eq(givenContainerId), any(DockerClient.LogsParam.class)))
                .thenReturn(mockLogStream);
        when(mockLogStream.readFully()).thenReturn(expectedLogs);

        String actualLogs = dockerAdapter.readAllLogsUntilExit(givenContainerId);

        assertThat(actualLogs, is(expectedLogs));
    }

    @Test
    @DisplayName("Given a container ID," +
            "when reading that container's logs," +
            "and a DockerException is thrown," +
            "should throw a ReadLogsException")
    void readLogsDockerException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";

        doThrow(new DockerException("")).when(mockClient)
                .logs(eq(givenContainerId), any(DockerClient.LogsParam.class));

        assertThrows(ReadLogsException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }

    @Test
    @DisplayName("Given a container ID," +
            "when reading that container's logs," +
            "and a InterruptedException is thrown," +
            "should throw a ReadLogsException")
    void readLogsInterruptedException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";

        doThrow(new InterruptedException("")).when(mockClient)
                .logs(eq(givenContainerId), any(DockerClient.LogsParam.class));

        assertThrows(ReadLogsException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }

    @Test
    @DisplayName("Given a container ID," +
            "when reading that container's logs," +
            "and an unexpected Exception is thrown," +
            "should throw that exception")
    void readLogsUnexpectedException() throws DockerException, InterruptedException {
        String givenContainerId = "container_id";

        doThrow(new RuntimeException("")).when(mockClient)
                .logs(eq(givenContainerId), any(DockerClient.LogsParam.class));

        assertThrows(RuntimeException.class, () -> dockerAdapter.readAllLogsUntilExit(givenContainerId));
    }
}