package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import net.jqwik.api.Example;
import net.jqwik.api.Label;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pruning removes every dangling image on the Docker daemon that the tests run against, not
// just the ones these tests create.
public class DockerAdapterPruneDanglingImagesTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private final List<String> containerIds = new ArrayList<>();
    private final List<String> imageIds = new ArrayList<>();

    @BeforeContainer
    static void beforeContainer() {
        dockerClient = DockerClientFactory.createDefaultClient();
    }

    @AfterContainer
    static void afterContainer() {
        try {
            dockerClient.removeImageCmd(TEST_IMAGE).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @BeforeTry
    void setUp() throws InterruptedException {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
    }

    @AfterTry
    void tearDown() {
        for (String containerId : containerIds) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception ignored) {}
        }
        for (String imageId : imageIds) {
            try {
                dockerClient.removeImageCmd(imageId).withForce(true).exec();
            } catch (Exception ignored) {}
        }
    }

    @Example
    @Label("Given a dangling image that no container uses, " +
            "when pruning dangling images, " +
            "should remove it.")
    void pruneDanglingImage() {
        String imageId = danglingCopyOf(TEST_IMAGE);

        dockerAdapter.pruneDanglingImages();

        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(imageId).exec());
    }

    @Example
    @Label("Given a dangling image that a container uses, " +
            "when pruning dangling images, " +
            "should keep it.")
    void keepDanglingImageInUse() {
        String imageId = danglingCopyOf(TEST_IMAGE);
        containerIds.add(dockerClient.createContainerCmd(imageId).exec().getId());

        dockerAdapter.pruneDanglingImages();

        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(imageId).exec());
    }

    @Example
    @Label("Given a tagged image that no container uses, " +
            "when pruning dangling images, " +
            "should keep it.")
    void keepTaggedImage() {
        TestImageContainers.removeAll(dockerClient, TEST_IMAGE);

        dockerAdapter.pruneDanglingImages();

        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(TEST_IMAGE).exec());
    }

    // An image with no tag, and its own ID.
    private String danglingCopyOf(String image) {
        String containerId = dockerClient.createContainerCmd(image).exec().getId();
        try {
            String imageId = dockerClient.commitCmd(containerId)
                    .withLabels(Map.of("conjob.test.copy", UUID.randomUUID().toString()))
                    .exec();
            imageIds.add(imageId);
            return imageId;
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }
    }
}
