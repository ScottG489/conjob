package dci.core.job;

public class RunningJobsLimiter {
    private int runningJobsCount = 0;
    private static final int MAX_RUNNING_JOBS_COUNT = 5;

    public synchronized boolean tryIncrement() {
        if (runningJobsCount >= MAX_RUNNING_JOBS_COUNT) {
            return false;
        }

        runningJobsCount++;
        return true;
    }

    public synchronized void decrement() {
        runningJobsCount--;
    }
}
