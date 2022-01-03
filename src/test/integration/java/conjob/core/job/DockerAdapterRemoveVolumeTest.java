package conjob.core.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Volume;
import conjob.core.job.exception.RemoveVolumeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterRemoveVolumeTest {
    private static final String EXISTING_VOLUME_NAME = "conjob-test-volume-0df74500";
    private static final String NONEXISTENT_VOLUME_NAME = "volume-that-does-not-exist-870dbba3";
    private static DockerClient dockerClient;
    private DockerAdapter dockerAdapter;


    @BeforeAll
    static void beforeAll() throws DockerCertificateException, DockerException, InterruptedException {
        dockerClient = DefaultDockerClient.fromEnv().build();
        dockerClient.createVolume(Volume.builder().name(EXISTING_VOLUME_NAME).build());
    }

    @BeforeEach
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @Test
    @DisplayName("Given a volume that exists, " +
            "when removing that volume, " +
            "should finish successfully.")
    void removeVolumeSuccessfully() {
        dockerAdapter.removeVolume(EXISTING_VOLUME_NAME);
    }

    @Test
    @DisplayName("Given a volume that doesn't exist, " +
            "when removing that volume, " +
            "should throw a RemoveVolumeException.")
    void removeVolumeDockerException() {
        assertThrows(RemoveVolumeException.class, () -> dockerAdapter.removeVolume(NONEXISTENT_VOLUME_NAME));
    }
}
