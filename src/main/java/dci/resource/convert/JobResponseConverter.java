package dci.resource.convert;

import dci.api.JobResponse;
import dci.api.JobResultResponse;
import dci.api.JobRunResponse;
import dci.core.job.model.Job;
import dci.core.job.model.JobResult;
import dci.core.job.model.JobRun;

public class JobResponseConverter {
    public JobResponse from(Job job) {
        return new JobResponse(
                from(job.getJobRun()),
                from(job.getResult())
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
