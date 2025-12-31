package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.JobUpdateException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterPullImageTest {
    private static final String EXISTING_IMAGE_NAME = "tianon/true:latest";
    private static final String NONEXISTENT_IMAGE_NAME = "image-that-does-not-exist-bbd10827";
    private static DockerClient dockerClient;
    private DockerAdapter dockerAdapter;


    @BeforeAll
    static void beforeContainer()  {
        dockerClient = DockerClientFactory.createDefaultClient();
    }

    @BeforeEach
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @AfterEach
    void tearDown()   {
        dockerClient.listImagesCmd().withImageNameFilter("tianon/true").exec().stream().findFirst()
                .ifPresent((image) -> removeImage(image.getId()));
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
            dockerClient.removeImageCmd(imageId).withForce(true).exec();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
