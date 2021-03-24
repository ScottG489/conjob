package conjob.core.job.model;

import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;

public interface JobRunCreationStrategy {
    String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException, JobUpdateException;
}
