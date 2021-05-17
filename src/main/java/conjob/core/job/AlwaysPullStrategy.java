package conjob.core.job;

import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.JobRunConfig;
import lombok.Value;

@Value
public class AlwaysPullStrategy implements JobRunCreationStrategy {
    DockerAdapter dockerAdapter;

    @Override
    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException, JobUpdateException {
        dockerAdapter.pullImage(jobRunConfig.getJobName());
        return dockerAdapter.createJobRun(jobRunConfig);
    }
}
