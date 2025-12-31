package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import conjob.core.job.exception.JobUpdateException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

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

    private PullImageResultCallback setupPullImageMock(String imageName) throws Exception {
        PullImageCmd mockCmd = mock(PullImageCmd.class);
        PullImageResultCallback mockCallback = mock(PullImageResultCallback.class);
        when(mockClient.pullImageCmd(imageName)).thenReturn(mockCmd);
        when(mockCmd.start()).thenReturn(mockCallback);
        return mockCallback;
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "should finish successfully.")
    void pullImageSuccessfully(@ForAll String imageName) throws Exception {
        PullImageResultCallback mockCallback = setupPullImageMock(imageName);
        when(mockCallback.awaitCompletion()).thenReturn(mockCallback);

        dockerAdapter.pullImage(imageName);

        verify(mockCallback).awaitCompletion();
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an Exception is thrown, " +
            "should throw a JobUpdateException.")
    void pullImageException(@ForAll String imageName) throws Exception {
        PullImageResultCallback mockCallback = setupPullImageMock(imageName);
        when(mockCallback.awaitCompletion()).thenThrow(new RuntimeException(""));

        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage(imageName));
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an InterruptedException is thrown, " +
            "should throw a JobUpdateException.")
    void pullImageInterruptedException(@ForAll String imageName) throws Exception {
        PullImageResultCallback mockCallback = setupPullImageMock(imageName);
        when(mockCallback.awaitCompletion()).thenThrow(new InterruptedException(""));

        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage(imageName));
    }

    @Property
    @Label("Given an image name, " +
            "when pulling that image, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void pullImageUnexpectedException(@ForAll String imageName) throws Exception {
        PullImageResultCallback mockCallback = setupPullImageMock(imageName);
        when(mockCallback.awaitCompletion()).thenThrow(new RuntimeException(""));

        assertThrows(RuntimeException.class, () -> dockerAdapter.pullImage(imageName));
    }
}
