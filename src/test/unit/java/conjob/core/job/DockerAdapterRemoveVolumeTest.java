package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveVolumeCmd;
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

    private RemoveVolumeCmd setupRemoveVolumeMock(String volumeName) {
        RemoveVolumeCmd mockCmd = mock(RemoveVolumeCmd.class);
        when(mockClient.removeVolumeCmd(volumeName)).thenReturn(mockCmd);
        return mockCmd;
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "should finish successfully.")
    void removeVolumeSuccessfully(@ForAll String volumeName)  {
        RemoveVolumeCmd mockCmd = setupRemoveVolumeMock(volumeName);

        dockerAdapter.removeVolume(volumeName);

        verify(mockCmd).exec();
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "and an Exception is thrown, " +
            "should throw a RemoveVolumeException.")
    void removeVolumeException(@ForAll String volumeName)  {
        RemoveVolumeCmd mockCmd = setupRemoveVolumeMock(volumeName);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(RemoveVolumeException.class, () -> dockerAdapter.removeVolume(volumeName));
    }

    @Property
    @Label("Given a volume name, " +
            "when removing that volume, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void removeVolumeUnexpectedException(@ForAll String volumeName)  {
        RemoveVolumeCmd mockCmd = setupRemoveVolumeMock(volumeName);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(RuntimeException.class, () -> dockerAdapter.removeVolume(volumeName));
    }
}