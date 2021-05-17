package conjob.core.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.JobUpdateException;
import org.junit.jupiter.api.*;

import static com.spotify.docker.client.DockerClient.ListImagesParam.byName;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterPullImageTest {
    private static final String EXISTING_IMAGE_NAME = "tianon/true";
    private static final String NONEXISTENT_IMAGE_NAME = "image-that-does-not-exist-bbd10827";
    private static DockerClient dockerClient;
    private DockerAdapter dockerAdapter;


    @BeforeAll
    static void beforeContainer() throws DockerCertificateException {
        dockerClient = DefaultDockerClient.fromEnv().build();
    }

    @BeforeEach
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @AfterEach
    void tearDown() throws DockerException, InterruptedException {
        dockerClient.listImages(byName("tianon/true")).stream().findFirst()
                .ifPresent((image) -> removeImage(image.id()));
    }

    @Test
    @DisplayName("Given an image that exists, " +
            "when pulling that image, " +
            "should finish successfully.")
    void pullImageSuccessfully() throws JobUpdateException {
        dockerAdapter.pullImage(EXISTING_IMAGE_NAME);
    }

    @Test
    @DisplayName("Given an image that doesn't exist, " +
            "when pulling that image, " +
            "should throw a JobUpdateException.")
    void pullImageDockerException() {
        assertThrows(JobUpdateException.class, () -> dockerAdapter.pullImage(NONEXISTENT_IMAGE_NAME));
    }

    private void removeImage(String imageId) {
        try {
            dockerClient.removeImage(imageId, true, false);
        } catch (DockerException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
