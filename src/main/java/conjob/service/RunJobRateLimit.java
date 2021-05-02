package conjob.service;

import com.codahale.metrics.SlidingTimeWindowReservoir;
import conjob.config.JobConfig;

import java.util.concurrent.TimeUnit;

public class RunJobRateLimit implements RunJobLimitMeter {
    private final SlidingTimeWindowReservoir requestTimeWindow;
    private final JobConfig.LimitConfig limitConfig;

    public RunJobRateLimit(JobConfig.LimitConfig limitConfig) {
        this.limitConfig = limitConfig;
        requestTimeWindow = new SlidingTimeWindowReservoir(1, TimeUnit.SECONDS);
    }

    @Override
    public synchronized boolean isAtLimit() {
        return requestTimeWindow.getSnapshot().getValues().length >= limitConfig.getMaxGlobalRequestsPerSecond();
    }

    @Override
    public synchronized void countRun() {
        requestTimeWindow.update(1);
    }

    @Override
    public synchronized void onJobComplete() {
    }
}
