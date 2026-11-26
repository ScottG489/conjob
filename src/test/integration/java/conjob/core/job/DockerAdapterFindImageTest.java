package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import net.jqwik.api.Example;
import net.jqwik.api.Label;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class DockerAdapterFindImageTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static final String NONEXISTENT_IMAGE = "local/image-that-does-not-exist-bbd10827:latest";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private String otherTag;

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
            if (otherTag != null)
                dockerClient.removeImageCmd(otherTag).exec();
        } catch (Exception ignored) {}
    }

    @Example
    @Label("Given an image, " +
            "when finding its ID by name, " +
            "should return its ID.")
    void findImageId() {
        String expectedImageId = dockerClient.inspectImageCmd(TEST_IMAGE).exec().getId();

        assertThat(dockerAdapter.findImageId(TEST_IMAGE), is(Optional.of(expectedImageId)));
    }

    @Example
    @Label("Given an image that doesn't exist, " +
            "when finding its ID, " +
            "should return nothing.")
    void findImageIdNonexistent() {
        assertThat(dockerAdapter.findImageId(NONEXISTENT_IMAGE), is(Optional.empty()));
    }

    @Example
    @Label("Given an image with two tags, " +
            "when finding its tags by ID, " +
            "should return both tags.")
    void findImageTags() {
        String imageId = dockerClient.inspectImageCmd(TEST_IMAGE).exec().getId();
        String otherRepository = "conjob-test-other-tag-" + UUID.randomUUID();
        dockerClient.tagImageCmd(imageId, otherRepository, "latest").exec();
        otherTag = otherRepository + ":latest";

        assertThat(dockerAdapter.findImageTags(imageId).orElseThrow(), containsInAnyOrder(TEST_IMAGE, otherTag));
    }

    @Example
    @Label("Given an image that doesn't exist, " +
            "when finding its tags, " +
            "should return nothing.")
    void findImageTagsNonexistent() {
        assertThat(dockerAdapter.findImageTags(NONEXISTENT_IMAGE), is(Optional.empty()));
    }
}
