package conjob.core.job.model;

import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.CreateJobRunException;
import lombok.Value;

@Value
public class NeverPullStrategy implements JobRunCreationStrategy {
    DockerAdapter dockerAdapter;

    @Override
    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        return dockerAdapter.createJobRun(jobRunConfig);
    }
}
