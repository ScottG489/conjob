package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.ListContainersException;
import conjob.core.job.exception.RemoveContainerException;
import conjob.core.job.model.ContainerImage;
import conjob.core.job.model.StoppedContainer;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

// Removes job containers and images that are no longer needed. Removing an image can take a
// while, so it's all done in the background instead of before a job's response is sent, one
// task at a time in the order they were queued.
@Slf4j
public class JobCleaner {
    // A job queues its cleanup within seconds of its container stopping, once it has read the
    // container's output. A sweep skips containers that stopped any later than this before the
    // sweep was queued, so it can't remove a container whose output is still being read, and
    // the cleanups queued ahead of it have already handled the rest.
    private static final Duration SETTLE_TIME = Duration.ofMinutes(1);

    private final DockerAdapter dockerAdapter;
    private final ImageRemover imageRemover;
    private final Executor cleanupExecutor;
    private final Clock clock;

    // cleanupExecutor has to run tasks one at a time, in the order they were queued.
    public JobCleaner(DockerAdapter dockerAdapter, ImageRemover imageRemover, Executor cleanupExecutor, Clock clock) {
        this.dockerAdapter = dockerAdapter;
        this.imageRemover = imageRemover;
        this.cleanupExecutor = cleanupExecutor;
        this.clock = clock;
    }

    public void cleanUpLater(String containerId, boolean remove, boolean removeImage) {
        queue("container '" + containerId + "'", () -> cleanUp(containerId, remove, removeImage));
    }

    // For a job whose container couldn't be created, after its image may have been pulled.
    public void removeImageLater(String imageName) {
        queue("image '" + imageName + "'", () -> imageRemover.removeByName(imageName));
    }

    // Cleans up after jobs whose own cleanup failed, or never ran because conjob stopped while
    // they ran, then removes dangling images.
    public void sweepLater() {
        Instant queuedAt = clock.instant();
        queue("leftover containers", () -> sweep(queuedAt));
    }

    private void queue(String what, Runnable cleanup) {
        try {
            cleanupExecutor.execute(cleanup);
        } catch (RejectedExecutionException e) {
            log.warn("Not cleaning up {} since conjob is shutting down", what);
        }
    }

    private void cleanUp(String containerId, boolean remove, boolean removeImage) {
        // Find the image through the container rather than by the job's image name, since the
        // name may have moved to a newer image since the container was created.
        ContainerImage image = removeImage ? findImage(containerId) : null;
        // A container holds on to its image's layers, so it's removed first.
        if (remove) {
            removeContainer(containerId);
        }
        if (image != null) {
            imageRemover.remove(image);
        }
    }

    private void sweep(Instant queuedAt) {
        Instant settledBefore = queuedAt.minus(SETTLE_TIME);
        try {
            List<StoppedContainer> leftovers = dockerAdapter.listStoppedContainersToRemove().stream()
                    .filter(container -> container.getStoppedAt().isBefore(settledBefore))
                    .toList();
            if (!leftovers.isEmpty()) {
                log.info("Removing {} leftover job containers", leftovers.size());
            }
            leftovers.forEach(container -> cleanUp(container.getId(), true, container.isRemoveImage()));
        } catch (ListContainersException e) {
            log.warn("Problem listing leftover job containers: {}", e.getMessage(), e);
        }
        imageRemover.removeDangling();
    }

    private ContainerImage findImage(String containerId) {
        try {
            return dockerAdapter.getContainerImage(containerId);
        } catch (InspectContainerException e) {
            log.warn("Problem looking up the image of container '{}'; image will not be removed: {}",
                    containerId, e.getMessage(), e);
            return null;
        }
    }

    private void removeContainer(String containerId) {
        try {
            dockerAdapter.removeContainer(containerId);
        } catch (RemoveContainerException e) {
            log.warn("Problem removing container: {}", e.getMessage(), e);
        }
    }
}
