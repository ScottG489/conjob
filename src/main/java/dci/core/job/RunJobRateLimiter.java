package dci.core.job;

import com.codahale.metrics.SlidingTimeWindowReservoir;

import java.util.concurrent.TimeUnit;

public class RunJobRateLimiter {
    private final SlidingTimeWindowReservoir requestTimeWindow =
            new SlidingTimeWindowReservoir(1, TimeUnit.SECONDS);
    private static final int MAX_RUN_JOB_REQUESTS_IN_WINDOW = 5;

    private int runningJobsCount = 0;
    private static final int MAX_RUNNING_JOBS_COUNT = 5;

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
