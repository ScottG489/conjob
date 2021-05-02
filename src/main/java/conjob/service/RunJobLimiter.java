package conjob.service;

import java.util.Arrays;
import java.util.List;

public class RunJobLimiter {
    private List<RunJobLimitMeter> runJobLimiters;


    public RunJobLimiter(RunJobLimitMeter... runJobLimitMeters) {
        this.runJobLimiters = Arrays.asList(runJobLimitMeters);
    }

    public synchronized boolean isLimitingOrIncrement() {
        if (runJobLimiters.stream().anyMatch(RunJobLimitMeter::isAtLimit)) {
            return true;
        }
        runJobLimiters.forEach(RunJobLimitMeter::countRun);
        return false;
    }

    public synchronized void markJobRunComplete() {
        runJobLimiters.forEach(RunJobLimitMeter::onJobComplete);
    }
}
