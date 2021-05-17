package conjob.core.job;

import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import lombok.Value;

@Value
public class NeverPullStrategy implements JobRunCreationStrategy {
    DockerAdapter dockerAdapter;

    @Override
    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        return dockerAdapter.createJobRun(jobRunConfig);
    }
}
