package conjob.service;

import java.util.concurrent.*;

public class JobRunner {
    private final DockerAdapter dockerAdapter;

    public JobRunner(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public JobRunOutcome runContainer(String containerId, long timeoutSeconds, int killTimeoutSeconds) {
        Long exitStatusCode;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Long> future = executor.submit(new WaitForContainer(dockerAdapter, containerId));
        try {
            exitStatusCode = future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException ignored) {
            try {
                exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeoutSeconds);
                // The container could finish naturally before the job timeout but before the stop-to-kill timeout.
                // TODO: Should this be 0L or existStatusCode? If above could have succeeded then we want the latter.
                exitStatusCode = wasStoppedOrKilled(exitStatusCode) ? -1 : 0L;
            } catch (StopJobRunException e) {
                exitStatusCode = -1L;
            }
        // TODO: Does this need to be in a finally block?
        } finally {
            executor.shutdownNow();
        }

        String output;
        try {
            output = dockerAdapter.readAllLogsUntilExit(containerId);
        } catch (ReadLogsException e) {
            output = "";
        }
        return new JobRunOutcome(exitStatusCode, output);
    }

    private boolean wasStoppedOrKilled(Long exitCode) {
        final int SIGKILL = 137;
        final int SIGTERM = 143;
        return exitCode == SIGKILL || exitCode == SIGTERM;
    }

    static class WaitForContainer implements Callable<Long> {
        private final DockerAdapter dockerAdapter;
        private final String containerId;

        public WaitForContainer(DockerAdapter dockerClient, String containerId) {
            this.dockerAdapter = dockerClient;
            this.containerId = containerId;
        }

        @Override
        public Long call() throws RunJobException {
            return dockerAdapter.startContainerThenWaitForExit(containerId);
        }
    }
}
