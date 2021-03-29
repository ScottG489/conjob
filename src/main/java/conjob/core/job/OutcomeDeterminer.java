package conjob.core.job;

import conjob.core.job.exception.UnknownOutcomeStatusCodeException;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.JobRunOutcome;

public class OutcomeDeterminer {
    private static final int TIMED_OUT_EXIT_CODE = -1;

    public JobRunConclusion determineOutcome(JobRunOutcome outcome) {
        JobRunConclusion jobRunConclusion;
        if (outcome.getExitStatusCode() > 0) {
            jobRunConclusion = JobRunConclusion.FAILURE;
        } else if (outcome.getExitStatusCode() == 0) {
            jobRunConclusion = JobRunConclusion.SUCCESS;
        } else if (outcome.getExitStatusCode() == TIMED_OUT_EXIT_CODE) {
            jobRunConclusion = JobRunConclusion.TIMED_OUT;
        } else {
            throw new UnknownOutcomeStatusCodeException();
        }

        return jobRunConclusion;
    }
}
