package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.exception.RemoveVolumeException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterRemoveVolumeTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "should finish successfully.")
    void removeVolumeSuccessfully(@ForAll String volumeName) throws DockerException, InterruptedException {
        dockerAdapter.removeVolume(volumeName);
        verify(mockClient).removeVolume(volumeName);
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "and a DockerException is thrown, " +
            "should throw a RemoveVolumeException.")
    void removeVolumeDockerException(@ForAll String volumeName) throws DockerException, InterruptedException {
        doThrow(new DockerException("")).when(mockClient).removeVolume(volumeName);

        assertThrows(RemoveVolumeException.class, () -> dockerAdapter.removeVolume(volumeName));
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "and an InterruptedException is thrown, " +
            "should throw a RemoveVolumeException.")
    void removeVolumeInterruptedException(@ForAll String volumeName) throws DockerException, InterruptedException {
        doThrow(new InterruptedException("")).when(mockClient).removeVolume(volumeName);

        assertThrows(RemoveVolumeException.class, () -> dockerAdapter.removeVolume(volumeName));
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void removeVolumeUnexpectedException(@ForAll String volumeName) throws DockerException, InterruptedException {
        doThrow(new RuntimeException("")).when(mockClient).removeVolume(volumeName);

        assertThrows(RuntimeException.class, () -> dockerAdapter.removeVolume(volumeName));
    }
}