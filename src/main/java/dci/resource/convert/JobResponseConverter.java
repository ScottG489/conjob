package dci.resource.convert;

import dci.api.JobResponse;
import dci.api.JobResultResponse;
import dci.api.JobRunResponse;
import dci.core.job.model.Job;
import dci.core.job.model.JobResult;
import dci.core.job.model.JobRun;

public class JobResponseConverter {
    public JobResponse from(Job job) {
        String message = "";

        if (job.getResult().equals(JobResult.FINISHED)) {
            message = "Job has concluded. Check job run for outcome.";
        } else if (job.getResult().equals(JobResult.NOT_FOUND)){
            message = "Image not found.";
        } else if (job.getResult().equals(JobResult.KILLED)) {
            message = "Job exceeded server configured maximum duration.";
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
        return JobResultResponse.valueOf(result.name());
    }
}
