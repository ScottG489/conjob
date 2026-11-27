package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import conjob.core.job.model.StoppedContainer;
import net.jqwik.api.Example;
import net.jqwik.api.Label;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DockerAdapterListStoppedContainersToRemoveTest {
    private static final String TEST_IMAGE = "scottg489/test-support-job:latest";
    private static final String QUICK_RUN_CMD = "0|||0";
    private static final String LONG_RUN_CMD = "60|||0";
    private static DockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private final List<String> containerIds = new ArrayList<>();

    @BeforeContainer
    static void beforeContainer() throws InterruptedException {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.pullImageCmd(TEST_IMAGE).start().awaitCompletion();
    }

    @AfterContainer
    static void afterContainer() {
        try {
            dockerClient.removeImageCmd(TEST_IMAGE).withForce(true).exec();
        } catch (Exception ignored) {}
    }

    @BeforeTry
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
    }

    @AfterTry
    void tearDown() {
        for (String containerId : containerIds) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception ignored) {}
        }
    }

    @Example
    @Label("Given job containers that exited, never started, are still running, or are kept, " +
            "when listing stopped containers to remove, " +
            "should list the exited and never started ones that remove is set for, " +
            "with whether to remove their image, " +
            "and when they stopped.")
    void listStoppedContainersToRemove() throws Exception {
        String exitedRemoveImage = createJobRun(QUICK_RUN_CMD, true, true);
        String neverStarted = createJobRun(QUICK_RUN_CMD, true, false);
        String running = createJobRun(LONG_RUN_CMD, true, true);
        String exitedKept = createJobRun(QUICK_RUN_CMD, false, false);
        dockerAdapter.startContainerThenWaitForExit(exitedRemoveImage);
        dockerAdapter.startContainerThenWaitForExit(exitedKept);
        dockerAdapter.startContainer(running);

        Map<String, StoppedContainer> listed = dockerAdapter.listStoppedContainersToRemove().stream()
                .collect(Collectors.toMap(StoppedContainer::getId, Function.identity()));

        assertThat(listed.keySet(), hasItems(exitedRemoveImage, neverStarted));
        assertThat(listed.keySet(), not(hasItems(running)));
        assertThat(listed.keySet(), not(hasItems(exitedKept)));
        assertThat(listed.get(exitedRemoveImage).isRemoveImage(), is(true));
        assertThat(listed.get(neverStarted).isRemoveImage(), is(false));
        InspectContainerResponse exited = dockerClient.inspectContainerCmd(exitedRemoveImage).exec();
        assertThat(listed.get(exitedRemoveImage).getStoppedAt(), is(Instant.parse(exited.getState().getFinishedAt())));
        InspectContainerResponse created = dockerClient.inspectContainerCmd(neverStarted).exec();
        assertThat(listed.get(neverStarted).getStoppedAt(), is(Instant.parse(created.getCreated())));
    }

    private String createJobRun(String input, boolean remove, boolean removeImage) throws CreateJobRunException {
        String containerId = dockerAdapter.createJobRun(
                new JobRunConfig(TEST_IMAGE, input, "cache-" + UUID.randomUUID(), null, false, remove, removeImage));
        containerIds.add(containerId);
        return containerId;
    }
}
