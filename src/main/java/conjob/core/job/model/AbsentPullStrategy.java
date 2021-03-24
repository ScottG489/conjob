package conjob.core.job.model;

import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import lombok.Value;

@Value
public class AbsentPullStrategy implements JobRunCreationStrategy {
    DockerAdapter dockerAdapter;

    @Override
    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException, JobUpdateException {
        String jobId;
        try {
            jobId = dockerAdapter.createJobRun(jobRunConfig);
        } catch (CreateJobRunException e) {
            dockerAdapter.pullImage(jobRunConfig.getJobName());
            jobId = dockerAdapter.createJobRun(jobRunConfig);
        }
        return jobId;
    }
}
