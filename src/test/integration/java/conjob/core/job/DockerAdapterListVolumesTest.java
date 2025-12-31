package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
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
    private static DockerClient dockerClient;

    @BeforeAll
    static void beforeAll() {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.createVolumeCmd().withName(EXISTING_VOLUME_NAME).exec();
    }

    @AfterAll
    static void afterAll()   {
        dockerClient.removeVolumeCmd(EXISTING_VOLUME_NAME).exec();
    }

    @Test
    @DisplayName("Given available volumes, " +
            "when listing all volume names, " +
            "should contain the name of an existing volume.")
    void listVolumesSuccess() {
        DockerAdapter adapter = new DockerAdapter(dockerClient);

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames, hasItem(EXISTING_VOLUME_NAME));
    }

    @Test
    @DisplayName("Given available volumes, " +
            "when listing all volume names, " +
            "should not contain the name of a non-existent volume.")
    void readLogsUnexpectedException() {
        DockerAdapter adapter = new DockerAdapter(dockerClient);

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames, not(hasItem("volume-that-does-not-exist-bbd10827")));
    }
}
