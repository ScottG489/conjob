package conjob.service;

import conjob.api.JobResponse;
import conjob.api.JobResultResponse;
import conjob.api.JobRunResponse;
import conjob.core.job.model.Job;
import conjob.core.job.model.JobResult;
import conjob.core.job.model.JobRun;

import java.util.Map;

public class JobResponseConverter {
    private static final Map<JobResult, JobResultResponse> jobResultToJobResultResponse = Map.of(
            JobResult.FINISHED, JobResultResponse.FINISHED,
            JobResult.KILLED, JobResultResponse.KILLED,
            JobResult.NOT_FOUND, JobResultResponse.NOT_FOUND,
            JobResult.REJECTED, JobResultResponse.REJECTED
    );

    public JobResponse from(Job job) {
        String message = "";

        if (job.getResult().equals(JobResult.FINISHED)) {
            message = "Job has concluded. Check job run for outcome.";
        } else if (job.getResult().equals(JobResult.NOT_FOUND)) {
            message = "Image not found.";
        } else if (job.getResult().equals(JobResult.KILLED)) {
            message = "Job exceeded maximum allowed duration.";
        } else if (job.getResult().equals(JobResult.REJECTED)) {
            message = "Concurrent job limit exceeded. Please wait then try again.";
        } else {
            message = "Unknown outcome.";
        }

        return new JobResponse(
                from(job.getJobRun()),
                from(job.getResult()),
                message
        );
    }

    public JobRunResponse from(JobRun jobRun) {
        return new JobRunResponse(
                jobRun.getOutput(),
                jobRun.getExitCode()
        );
    }

    public JobResultResponse from(JobResult result) {
        return jobResultToJobResultResponse.get(result);
    }
}
