package conjob.service;

import com.codahale.metrics.SlidingTimeWindowReservoir;
import conjob.config.JobConfig;

import java.util.concurrent.TimeUnit;

public class RunJobRateLimiter {
    private final JobConfig.LimitConfig limitConfig;

    private final SlidingTimeWindowReservoir requestTimeWindow =
            new SlidingTimeWindowReservoir(1, TimeUnit.SECONDS);
    private int runningJobsCount = 0;

    public RunJobRateLimiter(JobConfig.LimitConfig limitConfig) {
        this.limitConfig = limitConfig;
    }

    public synchronized boolean isAtLimit() {
        if (runningJobsCount >= limitConfig.getMaxConcurrentRuns()
                || requestTimeWindow.getSnapshot().getValues().length >= limitConfig.getMaxGlobalRequestsPerSecond()) {
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
