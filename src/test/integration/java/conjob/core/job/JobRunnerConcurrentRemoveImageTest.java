package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JobRunnerConcurrentRemoveImageTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static final String SHORT_RUN_CMD = "1|||0";
    private static final String LONG_RUN_CMD = "5|||0";

    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private JobRunner jobRunner;
    private final List<String> imageIdsToRemove = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        dockerClient = DockerClientFactory.createDefaultClient();
    }

    @AfterAll
    static void afterAll() {
        try {
            dockerClient.removeImageCmd(TEST_IMAGE).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
        jobRunner = new JobRunner(dockerAdapter);
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
    }

    @AfterEach
    void tearDown() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withAncestorFilter(List.of(TEST_IMAGE))
                    .exec();
            for (Container container : containers) {
                try {
                    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        for (String imageId : imageIdsToRemove) {
            try {
                dockerClient.removeImageCmd(imageId).withForce(true).exec();
            } catch (Exception ignored) {}
        }
    }

    @Test
    void concurrentRunsRemoveImageFully() throws Exception {
        String imageId = dockerClient.inspectImageCmd(TEST_IMAGE).exec().getId();
        imageIdsToRemove.add(imageId);

        // Stagger so the short-running container finishes first and calls
        // removeImage while the longer ones still reference the image.
        runConcurrently(List.of(
                createContainer(TEST_IMAGE, SHORT_RUN_CMD),
                createContainer(TEST_IMAGE, LONG_RUN_CMD),
                createContainer(TEST_IMAGE, LONG_RUN_CMD)));

        // Inspect by ID to assert the image data is gone, not just untagged.
        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(imageId).exec());
    }

    @Test
    void tagMovedDuringRunRemovesBothImages() throws Exception {
        String repository = "conjob-test-moving-tag-" + UUID.randomUUID();
        String movingTag = repository + ":latest";
        String oldImageId = commitCopyOf(TEST_IMAGE);
        String newImageId = commitCopyOf(TEST_IMAGE);

        dockerClient.tagImageCmd(oldImageId, repository, "latest").exec();
        String oldImageContainerId = createContainer(movingTag, LONG_RUN_CMD);
        // Like a new push of the same tag while the first job is still running.
        dockerClient.tagImageCmd(newImageId, repository, "latest").exec();
        String newImageContainerId = createContainer(movingTag, SHORT_RUN_CMD);

        runConcurrently(List.of(oldImageContainerId, newImageContainerId));

        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(oldImageId).exec());
        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(newImageId).exec());
    }

    private void runConcurrently(List<String> containerIds) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(containerIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (String containerId : containerIds) {
                futures.add(executor.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    jobRunner.runContainer(
                            containerId, Long.MAX_VALUE, Integer.MAX_VALUE, true);
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // A separate image, with its own ID, that runs the same as the given one.
    private String commitCopyOf(String image) {
        String containerId = dockerClient.createContainerCmd(image).exec().getId();
        try {
            String imageId = dockerClient.commitCmd(containerId)
                    .withLabels(Map.of("conjob.test.copy", UUID.randomUUID().toString()))
                    .exec();
            imageIdsToRemove.add(imageId);
            return imageId;
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    private String createContainer(String image, String cmd) throws CreateJobRunException {
        JobRunConfig jobRunConfig = new JobRunConfig(
                image, cmd, "cache-" + UUID.randomUUID(), null, false, true);
        return dockerAdapter.createJobRun(jobRunConfig);
    }
}
