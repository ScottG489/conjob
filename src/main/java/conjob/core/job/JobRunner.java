package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.RemoveContainerException;
import conjob.core.job.exception.RemoveImageException;
import conjob.core.job.exception.RunJobException;
import conjob.core.job.exception.StopJobRunException;
import conjob.core.job.model.JobRunOutcome;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class JobRunner {
    private static final long LOG_COMPLETION_TIMEOUT_SECONDS = 5;

    private final DockerAdapter dockerAdapter;

    public JobRunner(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public JobRunOutcome runContainer(String containerId, long timeoutSeconds, int killTimeoutSeconds, boolean remove, boolean removeImage) {
        // Remove the image by ID rather than by tag. Other jobs may be running the same
        // image, or its tag may have moved to a newer image since this container was created.
        String imageId = removeImage ? findImageId(containerId) : null;

        try {
            return run(containerId, timeoutSeconds, killTimeoutSeconds);
        } finally {
            // The container is only removed here, after its exit code and logs have been read,
            // and before its image, since a container holds on to its image's layers.
            if (remove) {
                removeContainer(containerId);
            }
            if (imageId != null) {
                removeImage(imageId);
            }
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

    private String findImageId(String containerId) {
        try {
            return dockerAdapter.getContainerImageId(containerId);
        } catch (InspectContainerException e) {
            log.warn("Problem looking up image ID for container '{}'; image will not be removed: {}",
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

    private void removeImage(String imageId) {
        try {
            dockerAdapter.removeImage(imageId);
        } catch (RemoveImageException e) {
            log.warn("Problem removing image: {}", e.getMessage(), e);
        }
    }

    private boolean wasStoppedOrKilled(Long exitCode) {
        final int SIGKILL = 137;
        final int SIGTERM = 143;
        return exitCode == SIGKILL || exitCode == SIGTERM;
    }
}
