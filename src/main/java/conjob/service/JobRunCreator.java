package conjob.service;

import conjob.core.job.model.PullStrategy;

public class JobRunCreator {
    private final DockerAdapter dockerAdapter;

    public JobRunCreator(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public String createJob(JobRunConfig jobRunConfig, PullStrategy pullStrategy)
            throws JobUpdateException, CreateJobRunException {
        String jobId;

        switch (pullStrategy) {
            case NEVER:
                jobId = createJobRun(jobRunConfig);
                break;
            case ALWAYS:
                dockerAdapter.pullImage(jobRunConfig.getJobName());
                jobId = createJobRun(jobRunConfig);
                break;
            case ABSENT:
                try {
                    jobId = createJobRun(jobRunConfig);
                } catch (CreateJobRunException e) {
                    dockerAdapter.pullImage(jobRunConfig.getJobName());
                    jobId = createJobRun(jobRunConfig);
                }
                break;
            default:
                throw new RuntimeException("Unknown pull strategy: " + pullStrategy.name());
        }

        return jobId;
    }

    private String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        return dockerAdapter.createJobRun(jobRunConfig);
    }
}
