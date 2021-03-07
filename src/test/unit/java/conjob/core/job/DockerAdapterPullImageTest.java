package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.exception.JobUpdateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterPullImageTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Test
    @DisplayName("Given an image name," +
            "when pulling that image," +
            "should finish successfully")
    void pullImageSuccessfully() throws JobUpdateException, DockerException, InterruptedException {
        dockerAdapter.pullImage("image_name");
        verify(mockClient).pull("image_name");
    }

    @Test
    @DisplayName("Given an image name," +
            "when pulling that image," +
            "and a DockerException is thrown," +
            "should throw a JobUpdateException")
    void pullImageDockerException() throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).pull("image_name");

        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage("image_name"));
    }

    @Test
    @DisplayName("Given an image name," +
            "when pulling that image," +
            "and an InterruptedException is thrown," +
            "should throw a JobUpdateException")
    void pullImageInterruptedException() throws DockerException, InterruptedException {
        doThrow(new InterruptedException("")).when(mockClient).pull("image_name");

        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage("image_name"));
    }

    @Test
    @DisplayName("Given an image name," +
            "when pulling that image," +
            "and an unexpected Exception is thrown," +
            "should throw that exception")
    void pullImageUnexpectedException() throws DockerException, InterruptedException {
        doThrow(new RuntimeException("")).when(mockClient).pull("image_name");

        assertThrows(RuntimeException.class, () -> dockerAdapter.pullImage("image_name"));
    }
}