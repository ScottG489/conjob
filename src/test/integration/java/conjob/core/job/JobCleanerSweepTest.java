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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// A sweep also removes every dangling image on the Docker daemon that the tests run against.
public class JobCleanerSweepTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static final String QUICK_RUN_CMD = "0|||0";
    private static final String LONG_RUN_CMD = "60|||0";
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
    @ExpectedLogs({JobCleaner.class, ImageRemover.class})
    @Label("Given job containers left behind over a minute ago, " +
            "when sweeping, " +
            "should remove the ones that remove is set for, " +
            "and the images of the ones that removeImage is set for, " +
            "and leave the kept and running ones.")
    void sweepLeftovers() throws Exception {
        String imageToRemove = taggedCopyOf(TEST_IMAGE);
        String imageToKeep = taggedCopyOf(TEST_IMAGE);
        String removeWithImage = createJobRun(imageToRemove, QUICK_RUN_CMD, true, true);
        String removeWithoutImage = createJobRun(imageToKeep, QUICK_RUN_CMD, true, false);
        String kept = createJobRun(TEST_IMAGE, QUICK_RUN_CMD, false, false);
        String running = createJobRun(TEST_IMAGE, LONG_RUN_CMD, true, true);
        dockerAdapter.startContainerThenWaitForExit(removeWithImage);
        dockerAdapter.startContainerThenWaitForExit(removeWithoutImage);
        dockerAdapter.startContainerThenWaitForExit(kept);
        dockerAdapter.startContainer(running);

        sweepAt(Instant.now().plus(Duration.ofMinutes(2)));

        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(removeWithImage).exec());
        assertThrows(NotFoundException.class, () -> dockerClient.inspectImageCmd(imageToRemove).exec());
        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(removeWithoutImage).exec());
        assertDoesNotThrow(() -> dockerClient.inspectImageCmd(imageToKeep).exec());
        assertDoesNotThrow(() -> dockerClient.inspectContainerCmd(kept).exec());
        assertDoesNotThrow(() -> dockerClient.inspectContainerCmd(running).exec());
    }

    @Example
    @ExpectedLogs({JobCleaner.class, ImageRemover.class})
    @Label("Given a job container that stopped just now, " +
            "when sweeping, " +
            "should leave it to the job's own cleanup.")
    void sweepSkipsJustStopped() throws Exception {
        String justStopped = createJobRun(TEST_IMAGE, QUICK_RUN_CMD, true, false);
        dockerAdapter.startContainerThenWaitForExit(justStopped);

        sweepAt(Instant.now());

        assertDoesNotThrow(() -> dockerClient.inspectContainerCmd(justStopped).exec());
    }

    private void sweepAt(Instant now) {
        JobCleaner jobCleaner = new JobCleaner(
                dockerAdapter,
                new ImageRemover(dockerAdapter, new ReentrantReadWriteLock().writeLock()),
                Runnable::run,
                Clock.fixed(now, ZoneOffset.UTC));
        jobCleaner.sweepLater();
    }

    private String createJobRun(String image, String input, boolean remove, boolean removeImage) throws CreateJobRunException {
        String containerId = dockerAdapter.createJobRun(
                new JobRunConfig(image, input, "cache-" + UUID.randomUUID(), null, false, remove, removeImage));
        containerIds.add(containerId);
        return containerId;
    }

    // A separate image, with its own ID and only one tag, that runs the same as the given one.
    private String taggedCopyOf(String image) {
        String repository = "conjob-test-sweep-" + UUID.randomUUID();
        String containerId = dockerClient.createContainerCmd(image).exec().getId();
        try {
            imageIds.add(dockerClient.commitCmd(containerId)
                    .withRepository(repository)
                    .withTag("latest")
                    .withLabels(Map.of("conjob.test.copy", UUID.randomUUID().toString()))
                    .exec());
            return repository + ":latest";
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }
    }
}
