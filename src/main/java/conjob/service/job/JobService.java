package conjob.service.job;

import conjob.config.JobConfig;
import conjob.core.job.*;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.JobRunConfig;
import conjob.core.job.model.JobRunOutcome;
import conjob.core.secrets.SecretsStore;
import conjob.core.secrets.SecretsStoreException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobService {

    private final RunJobLimiter runJobLimiter;
    private final JobConfig.LimitConfig limitConfig;
    private final JobRunCreationStrategyDeterminer jobRunCreationStrategyDeterminer;
    private final JobRunner jobRunner;
    private final JobRunConfigCreator jobRunConfigCreator;
    private final ConfigUtil configUtil;
    private final SecretsStore secretsStore;
    private final OutcomeDeterminer outcomeDeterminer;
    private final ImageTagEnsurer imageTagEnsurer;

    public JobService(
            RunJobLimiter runJobLimiter,
            JobConfig.LimitConfig limitConfig,
            SecretsStore secretsStore,
            JobRunCreationStrategyDeterminer jobRunCreationStrategyDeterminer,
            JobRunner jobRunner,
            JobRunConfigCreator jobRunConfigCreator,
            OutcomeDeterminer outcomeDeterminer,
            ConfigUtil configUtil,
            ImageTagEnsurer imageTagEnsurer) {
        this.runJobLimiter = runJobLimiter;
        this.limitConfig = limitConfig;
        this.secretsStore = secretsStore;
        this.jobRunCreationStrategyDeterminer = jobRunCreationStrategyDeterminer;
        this.jobRunner = jobRunner;
        this.jobRunConfigCreator = jobRunConfigCreator;
        this.outcomeDeterminer = outcomeDeterminer;
        this.configUtil = configUtil;
        this.imageTagEnsurer = imageTagEnsurer;
    }

    public JobRun runJob(String imageName, String input, String pullStrategyName, boolean useDockerCache, boolean remove) throws SecretsStoreException {
        imageName = imageTagEnsurer.hasTagOrLatest(imageName);
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        return runJob(imageName, input, pullStrategy, useDockerCache, remove);
    }

    private JobRun runJob(String imageName, String input, PullStrategy pullStrategy, boolean useDockerCache, boolean remove)
            throws SecretsStoreException {
        if (runJobLimiter.isLimitingOrIncrement()) {
            return new JobRun(JobRunConclusion.REJECTED, "", -1);
        }

        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());

        JobRunConfig jobRunConfig = getJobRunConfig(imageName, input, useDockerCache, remove);
        JobRunCreationStrategy jobRunCreationStrategy =
                jobRunCreationStrategyDeterminer.determineStrategy(pullStrategy);

        String jobId;
        try {
            jobId = jobRunCreationStrategy.createJobRun(jobRunConfig);
        } catch (CreateJobRunException | JobUpdateException ex) {
            log.warn("Problem creating job: {}", ex.getMessage(), ex);
            runJobLimiter.markJobRunComplete();
            return new JobRun(JobRunConclusion.NOT_FOUND, "", -1);
        }

        JobRunOutcome outcome = jobRunner
                .runContainer(jobId, maxTimeoutSeconds, maxKillTimeoutSeconds);
        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(outcome);

        runJobLimiter.markJobRunComplete();
        return new JobRun(jobRunConclusion, outcome.getOutput(), outcome.getExitStatusCode());
    }

    private JobRunConfig getJobRunConfig(String imageName, String input, boolean useDockerCache, boolean remove) throws SecretsStoreException {
        String correspondingSecretsVolumeName = configUtil.translateToSecretsVolumeName(imageName);
        String dockerCacheVolumeName = configUtil.translateToDockerCacheVolumeName(imageName);
        String secretsVolumeName = secretsStore
                .findSecrets(correspondingSecretsVolumeName)
                .orElse(null);

        return jobRunConfigCreator.getContainerConfig(imageName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache, remove);
    }
}
