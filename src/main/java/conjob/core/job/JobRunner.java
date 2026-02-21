package conjob.core.job;

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

    public JobRunOutcome runContainer(String containerId, long timeoutSeconds, int killTimeoutSeconds) {
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

    private boolean wasStoppedOrKilled(Long exitCode) {
        final int SIGKILL = 137;
        final int SIGTERM = 143;
        return exitCode == SIGKILL || exitCode == SIGTERM;
    }
}
