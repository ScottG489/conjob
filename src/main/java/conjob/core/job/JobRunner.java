package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.RemoveContainerException;
import conjob.core.job.exception.RunJobException;
import conjob.core.job.exception.StopJobRunException;
import conjob.core.job.model.ContainerImage;
import conjob.core.job.model.JobRunOutcome;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class JobRunner {
    private static final long LOG_COMPLETION_TIMEOUT_SECONDS = 5;

    private final DockerAdapter dockerAdapter;
    private final ImageRemover imageRemover;
    private final Executor cleanupExecutor;

    public JobRunner(DockerAdapter dockerAdapter, ImageRemover imageRemover, Executor cleanupExecutor) {
        this.dockerAdapter = dockerAdapter;
        this.imageRemover = imageRemover;
        this.cleanupExecutor = cleanupExecutor;
    }

    public JobRunOutcome runContainer(String containerId, long timeoutSeconds, int killTimeoutSeconds, boolean remove, boolean removeImage) {
        try {
            return run(containerId, timeoutSeconds, killTimeoutSeconds);
        } finally {
            if (remove || removeImage) {
                cleanUpLater(containerId, remove, removeImage);
            }
        }
    }

    // Removing a container and its image can take a while, so it's done in the background
    // instead of before the job's response is sent.
    private void cleanUpLater(String containerId, boolean remove, boolean removeImage) {
        try {
            cleanupExecutor.execute(() -> cleanUp(containerId, remove, removeImage));
        } catch (RejectedExecutionException e) {
            log.warn("Not cleaning up container '{}' since conjob is shutting down", containerId);
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

    private JobRunOutcome run(String containerId, long timeoutSeconds, int killTimeoutSeconds) {
        try {
            dockerAdapter.startContainer(containerId);
        } catch (RunJobException e) {
            log.warn("Problem starting job: {}", e.getMessage(), e);
            return new JobRunOutcome(-1L, "");
        }

        Long exitStatusCode;
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> logsFuture = executor.submit(() -> dockerAdapter.readAllLogsUntilExit(containerId));
            Future<Long> exitFuture = executor.submit(() -> dockerAdapter.waitForExit(containerId));

            try {
                exitStatusCode = exitFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException | InterruptedException ex) {
                log.warn("Problem finishing job: {}", ex.getMessage(), ex);
                try {
                    exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeoutSeconds);
                    // The container could finish naturally before the job timeout but before the stop-to-kill timeout.
                    exitStatusCode = wasStoppedOrKilled(exitStatusCode) ? -1 : exitStatusCode;
                } catch (StopJobRunException e) {
                    exitStatusCode = -1L;
                }
            }

            String output;
            try {
                output = logsFuture.get(LOG_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                output = "";
            }

            return new JobRunOutcome(exitStatusCode, output);
        } finally {
            executor.shutdownNow();
        }
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

    private boolean wasStoppedOrKilled(Long exitCode) {
        final int SIGKILL = 137;
        final int SIGTERM = 143;
        return exitCode == SIGKILL || exitCode == SIGTERM;
    }
}
