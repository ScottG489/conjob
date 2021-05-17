package conjob.service.job;

import conjob.config.JobConfig;
import lombok.Getter;

public class ConcurrentJobCountLimiter implements RunJobLimitMeter {
    @Getter
    private int currentlyRunningJobsCount = 0;
    @Getter
    private final JobConfig.LimitConfig limitConfig;

    public ConcurrentJobCountLimiter(JobConfig.LimitConfig limitConfig) {
        this.limitConfig = limitConfig;
    }

    @Override
    public synchronized boolean isAtLimit() {
        return currentlyRunningJobsCount >= limitConfig.getMaxConcurrentRuns();
    }

    @Override
    public synchronized void countRun() {
        currentlyRunningJobsCount++;
    }

    @Override
    public synchronized void onJobComplete() {
        if (currentlyRunningJobsCount > 0) currentlyRunningJobsCount--;
    }
}
