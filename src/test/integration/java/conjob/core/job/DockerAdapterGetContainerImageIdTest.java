package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import conjob.core.job.exception.InspectContainerException;
import net.jqwik.api.Example;
import net.jqwik.api.Label;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterGetContainerImageIdTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private String containerId;

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
        try {
            if (containerId != null)
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @Example
    @Label("Given a container, " +
            "when getting its image ID, " +
            "should return the ID of the image it was created from.")
    void getContainerImageId() {
        String expectedImageId = dockerClient.inspectImageCmd(TEST_IMAGE).exec().getId();
        containerId = dockerClient.createContainerCmd(TEST_IMAGE).exec().getId();

        assertThat(dockerAdapter.getContainerImageId(containerId), is(expectedImageId));
    }

    @Example
    @Label("Given a container ID that doesn't exist, " +
            "when getting its image ID, " +
            "should throw an InspectContainerException.")
    void getContainerImageIdNonexistent() {
        assertThrows(InspectContainerException.class,
                () -> dockerAdapter.getContainerImageId("container-that-does-not-exist-5e0c8a1f"));
    }
}
