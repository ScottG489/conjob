package conjob.core.job;

import com.codahale.metrics.SlidingTimeWindowReservoir;
import conjob.config.JobConfig;

import java.util.concurrent.TimeUnit;

public class RunJobRateLimiter {
    private final long MAX_RUN_JOB_REQUESTS_IN_WINDOW;
    private final long MAX_RUNNING_JOBS_COUNT;

    private final SlidingTimeWindowReservoir requestTimeWindow =
            new SlidingTimeWindowReservoir(1, TimeUnit.SECONDS);
    private int runningJobsCount = 0;

    public RunJobRateLimiter(JobConfig.LimitConfig limitConfig) {
        MAX_RUN_JOB_REQUESTS_IN_WINDOW = limitConfig.getMaxGlobalRequestsPerSecond();
        MAX_RUNNING_JOBS_COUNT = limitConfig.getMaxConcurrentRuns();
    }

    public synchronized boolean isAtLimit() {
        if (runningJobsCount >= MAX_RUNNING_JOBS_COUNT
                || requestTimeWindow.getSnapshot().getValues().length >= MAX_RUN_JOB_REQUESTS_IN_WINDOW) {
            return true;
        }

        runningJobsCount++;
        requestTimeWindow.update(1);
        return false;
    }

    public synchronized void decrementRunningJobsCount() {
        runningJobsCount--;
    }
}
