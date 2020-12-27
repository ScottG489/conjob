package conjob.service;

import conjob.core.job.model.PullStrategy;

import java.util.Optional;

public class JobRunCreator {
    private final DockerAdapter dockerAdapter;

    public JobRunCreator(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public Optional<String> createJob(JobRunConfig jobRunConfig, PullStrategy pullStrategy)
            throws JobUpdateException, CreateJobRunException {
        Optional<String> jobId;

        switch (pullStrategy) {
            case NEVER:
                jobId = maybeCreateJobRun(jobRunConfig);
                break;
            case ALWAYS:
                dockerAdapter.pullImage(jobRunConfig.getJobName());
                jobId = maybeCreateJobRun(jobRunConfig);
                break;
            case ABSENT:
                try {
                    jobId = maybeCreateJobRun(jobRunConfig);
                } catch (CreateJobRunException e) {
                    dockerAdapter.pullImage(jobRunConfig.getJobName());
                    jobId = maybeCreateJobRun(jobRunConfig);
                }
                break;
            default:
                throw new RuntimeException("Unknown pull strategy: " + pullStrategy.name());
        }

        return jobId;
    }

    private Optional<String> maybeCreateJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        return Optional.of(dockerAdapter.createJobRun(jobRunConfig));
    }
}
