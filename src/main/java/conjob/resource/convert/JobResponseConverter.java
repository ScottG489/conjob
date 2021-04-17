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

    private static final Map<JobRunConclusion, String> jobRunConclusionResponseMessage = Map.of(
            JobRunConclusion.SUCCESS, "Job run successful.",
            JobRunConclusion.FAILURE, "Job run failed.",
            JobRunConclusion.NOT_FOUND, "Job not found.",
            JobRunConclusion.TIMED_OUT, "Job exceeded maximum allowed duration.",
            JobRunConclusion.REJECTED, "Concurrent job limit exceeded. Please wait then try again."
    );

    public JobRunResponse from(JobRun jobRun) {
        return new JobRunResponse(
                from(jobRun.getConclusion()),
                jobRun.getOutput(),
                jobRun.getExitCode(),
                responseMessageFrom(jobRun.getConclusion())
        );
    }

    private String responseMessageFrom(JobRunConclusion conclusion) {
        return conclusion == null
                ? "Unknown outcome."
                : jobRunConclusionResponseMessage.getOrDefault(conclusion, "Unknown outcome.");
    }

    private JobRunConclusionResponse from(JobRunConclusion conclusion) {
        return conclusion == null
                ? JobRunConclusionResponse.UNKNOWN
                : jobResultToJobResultResponse.getOrDefault(conclusion, JobRunConclusionResponse.UNKNOWN);
    }
}
