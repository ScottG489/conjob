package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.exception.JobUpdateException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterPullImageTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "should finish successfully.")
    void pullImageSuccessfully(@ForAll String imageName) throws JobUpdateException, DockerException, InterruptedException {
        dockerAdapter.pullImage(imageName);
        verify(mockClient).pull(imageName);
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and a DockerException is thrown, " +
            "should throw a JobUpdateException.")
    void pullImageDockerException(@ForAll String imageName) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).pull(imageName);

        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage(imageName));
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an InterruptedException is thrown, " +
            "should throw a JobUpdateException.")
    void pullImageInterruptedException(@ForAll String imageName) throws DockerException, InterruptedException {
        doThrow(new InterruptedException("")).when(mockClient).pull(imageName);

        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage(imageName));
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void pullImageUnexpectedException(@ForAll String imageName) throws DockerException, InterruptedException {
        doThrow(new RuntimeException("")).when(mockClient).pull(imageName);

        assertThrows(RuntimeException.class, () -> dockerAdapter.pullImage(imageName));
    }
}