package conjob.core.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Volume;
import conjob.core.job.DockerAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

public class DockerAdapterListVolumesTest {
    private static final String EXISTING_VOLUME_NAME = "conjob-test-volume-870dbba3";
    private static DefaultDockerClient dockerClient;

    @BeforeAll
    static void beforeAll() throws DockerCertificateException, DockerException, InterruptedException {
        dockerClient = DefaultDockerClient.fromEnv().build();
        dockerClient.createVolume(Volume.builder().name(EXISTING_VOLUME_NAME).build());
    }

    @AfterAll
    static void afterAll() throws DockerException, InterruptedException {
        dockerClient.removeVolume(Volume.builder().name(EXISTING_VOLUME_NAME).build());
    }

    @Test
    @DisplayName("Given available volumes, " +
            "when listing all volume names, " +
            "should contain the name of an existing volume.")
    void listVolumesSuccess()
            throws DockerException, InterruptedException {
        DockerAdapter adapter = new DockerAdapter(dockerClient);

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames, hasItem(EXISTING_VOLUME_NAME));
    }

    @Test
    @DisplayName("Given available volumes, " +
            "when listing all volume names, " +
            "should not contain the name of a non-existent volume.")
    void readLogsUnexpectedException() throws DockerException, InterruptedException {
        DockerAdapter adapter = new DockerAdapter(dockerClient);

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames, not(hasItem("volume-that-does-not-exist-bbd10827")));
    }
}