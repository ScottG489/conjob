package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.Example;
import net.jqwik.api.Label;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import testsupport.ExpectedLogs;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JobRunnerConcurrentRemoveImageTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static final String SHORT_RUN_CMD = "1|||0";
    private static final String LONG_RUN_CMD = "5|||0";

    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private JobRunner jobRunner;
    private final List<String> imageIdsToRemove = new ArrayList<>();

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
        jobRunner = new JobRunner(
                dockerAdapter,
                new JobCleaner(
                        dockerAdapter,
                        new ImageRemover(dockerAdapter, new ReentrantReadWriteLock().writeLock()),
                        Runnable::run,
                        Clock.systemUTC()));
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
        TestImageContainers.removeAll(dockerClient, TEST_IMAGE);
    }

    @AfterTry
    void tearDown() {
        TestImageContainers.removeAll(dockerClient, TEST_IMAGE);
        for (String imageId : imageIdsToRemove) {
            try {
                dockerClient.removeImageCmd(imageId).withForce(true).exec();
            } catch (Exception ignored) {}
        }
    }

    // Jobs that finish while another job's container still uses the image can't remove it.
    @Example
    @ExpectedLogs(ImageRemover.class)
    @Label("Given several jobs running the same image at once, " +
            "when each one removes its image after its run, " +
            "the image should be fully removed once they all finish.")
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

    @Example
    @Label("Given a job running an image, " +
            "and the image's tag moving to another image that a second job runs, " +
            "when each job removes its image after its run, " +
            "both images should be fully removed.")
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
                            containerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);
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
                image, cmd, "cache-" + UUID.randomUUID(), null, false, false, false);
        return dockerAdapter.createJobRun(jobRunConfig);
    }
}
