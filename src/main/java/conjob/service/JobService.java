package conjob.service;

import conjob.config.JobConfig;
import conjob.core.job.*;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.*;
import conjob.core.secret.SecretStore;
import conjob.core.secret.SecretStoreException;

public class JobService {

    private final RunJobRateLimiter runJobRateLimiter;
    private final JobConfig.LimitConfig limitConfig;
    private final JobRunCreationStrategyDeterminer jobRunCreationStrategyDeterminer;
    private final JobRunner jobRunner;
    private final JobRunConfigCreator jobRunConfigCreator;
    private final ConfigUtil configUtil;
    private final SecretStore secretStore;
    private final OutcomeDeterminer outcomeDeterminer;

    public JobService(
            DockerAdapter dockerAdapter,
            RunJobRateLimiter runJobRateLimiter, JobConfig.LimitConfig limitConfig) {
        this.runJobRateLimiter = runJobRateLimiter;
        this.limitConfig = limitConfig;

        this.secretStore = new SecretStore(dockerAdapter);
        this.jobRunCreationStrategyDeterminer = new JobRunCreationStrategyDeterminer(dockerAdapter);
        this.jobRunner = new JobRunner(dockerAdapter);
        this.jobRunConfigCreator = new JobRunConfigCreator();
        this.outcomeDeterminer = new OutcomeDeterminer();
        this.configUtil = new ConfigUtil();
    }

    public JobRun runJob(String imageName, String input) throws SecretStoreException {
        return runJob(imageName, input, PullStrategy.ALWAYS.name());
    }

    public JobRun runJob(String imageName, String input, String pullStrategyName) throws SecretStoreException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        return runJob(imageName, input, pullStrategy);
    }

    private JobRun runJob(String imageName, String input, PullStrategy pullStrategy)
            throws SecretStoreException {
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());

        if (runJobRateLimiter.isAtLimit()) {
            return new JobRun(JobRunConclusion.REJECTED, "", -1);
        }

        JobRunConfig jobRunConfig = getJobRunConfig(imageName, input);
        JobRunCreationStrategy jobRunCreationStrategy =
                jobRunCreationStrategyDeterminer.determineStrategy(pullStrategy);

        String jobId;
        try {
            jobId = jobRunCreationStrategy.createJobRun(jobRunConfig);
        } catch (CreateJobRunException | JobUpdateException e2) {
            runJobRateLimiter.decrementRunningJobsCount();
            return new JobRun(JobRunConclusion.NOT_FOUND, "", -1);
        }

        JobRunOutcome outcome = jobRunner
                .runContainer(jobId, maxTimeoutSeconds, maxKillTimeoutSeconds);
        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(outcome);

        runJobRateLimiter.decrementRunningJobsCount();
        return new JobRun(jobRunConclusion, outcome.getOutput(), outcome.getExitStatusCode());
    }

    private JobRunConfig getJobRunConfig(String imageName, String input) throws SecretStoreException {
        String correspondingSecretsVolumeName = configUtil.translateToVolumeName(imageName);
        String secretId = secretStore
                .findSecret(correspondingSecretsVolumeName)
                .orElse(null);

        return jobRunConfigCreator.getContainerConfig(imageName, input, secretId);
    }
}
