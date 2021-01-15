package conjob.service;

import conjob.core.job.model.JobRunConclusion;

public class OutcomeDeterminer {
    private static final int TIMED_OUT_EXIT_CODE = -1;

    JobRunConclusion determineOutcome(JobRunOutcome outcome) {
        JobRunConclusion jobRunConclusion;
        if (outcome.getExitStatusCode() == TIMED_OUT_EXIT_CODE) {
            jobRunConclusion = JobRunConclusion.TIMED_OUT;
        } else if (outcome.getExitStatusCode() != 0) {
            jobRunConclusion = JobRunConclusion.FAILURE;
        } else {
            jobRunConclusion = JobRunConclusion.SUCCESS;
        }

        return jobRunConclusion;
    }
}
