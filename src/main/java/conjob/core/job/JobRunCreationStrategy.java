package conjob.core.job;

import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.JobRunConfig;

public interface JobRunCreationStrategy {
    String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException, JobUpdateException;
}
