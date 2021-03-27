package conjob.resource.convert;

import conjob.api.JobRunConclusionResponse;
import conjob.api.JobRunResponse;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;

import java.util.Map;

public class JobResponseConverter {
    private static final Map<JobRunConclusion, JobRunConclusionResponse> jobResultToJobResultResponse = Map.of(
            JobRunConclusion.SUCCESS, JobRunConclusionResponse.SUCCESS,
            JobRunConclusion.FAILURE, JobRunConclusionResponse.FAILURE,
            JobRunConclusion.NOT_FOUND, JobRunConclusionResponse.NOT_FOUND,
            JobRunConclusion.TIMED_OUT, JobRunConclusionResponse.TIMED_OUT,
            JobRunConclusion.REJECTED, JobRunConclusionResponse.REJECTED
    );

    public JobRunResponse from(JobRun jobRun) {
        String message;

        if (jobRun.getConclusion().equals(JobRunConclusion.SUCCESS)) {
            message = "Job run successful.";
        } else if (jobRun.getConclusion().equals(JobRunConclusion.FAILURE)) {
            message = "Job run failed.";
        } else if (jobRun.getConclusion().equals(JobRunConclusion.NOT_FOUND)) {
            message = "Job not found.";
        } else if (jobRun.getConclusion().equals(JobRunConclusion.TIMED_OUT)) {
            message = "Job exceeded maximum allowed duration.";
        } else if (jobRun.getConclusion().equals(JobRunConclusion.REJECTED)) {
            message = "Concurrent job limit exceeded. Please wait then try again.";
        } else {
            message = "Unknown outcome.";
        }

        return new JobRunResponse(
                from(jobRun.getConclusion()),
                jobRun.getOutput(),
                jobRun.getExitCode(),
                message
        );
    }

    public JobRunConclusionResponse from(JobRunConclusion conclusion) {
        return jobResultToJobResultResponse.get(conclusion);
    }
}
