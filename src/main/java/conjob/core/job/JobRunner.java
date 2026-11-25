package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.RemoveImageException;
import conjob.core.job.exception.RunJobException;
import conjob.core.job.exception.StopJobRunException;
import conjob.core.job.model.JobRunOutcome;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class JobRunner {
    private static final long LOG_COMPLETION_TIMEOUT_SECONDS = 5;
    private static final int CONTAINER_REMOVAL_TIMEOUT_SECONDS = 30;

    private final DockerAdapter dockerAdapter;

    public JobRunner(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public JobRunOutcome runContainer(String containerId, long timeoutSeconds, int killTimeoutSeconds, boolean removeImage) {
        // Resolve the image ID before startContainer to avoid two races:
        // (1) removing by tag instead of ID leaves dangling images under
        //     concurrent runs of the same image (force only untags; later
        //     finishers hit NotFoundException), and
        // (2) inspecting after startContainer would race autoRemove on
        //     quick-exit jobs (the extra round-trip can let the container
        //     vanish before wait/log streams are hooked up).
        String imageId = null;
        if (removeImage) {
            try {
                imageId = dockerAdapter.getContainerImageId(containerId);
            } catch (InspectContainerException e) {
                log.warn("Problem looking up image ID for container '{}'; image will not be removed: {}",
                        containerId, e.getMessage(), e);
            }
        }

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
            if (imageId != null) {
                dockerAdapter.waitForContainerRemoval(containerId, CONTAINER_REMOVAL_TIMEOUT_SECONDS);
                try {
                    dockerAdapter.removeImage(imageId);
                } catch (RemoveImageException e) {
                    log.warn("Problem removing image: {}", e.getMessage(), e);
                }
            }
        }
    }

    private boolean wasStoppedOrKilled(Long exitCode) {
        final int SIGKILL = 137;
        final int SIGTERM = 143;
        return exitCode == SIGKILL || exitCode == SIGTERM;
    }
}
