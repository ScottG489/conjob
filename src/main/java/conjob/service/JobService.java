package conjob.service;

import conjob.config.JobConfig;
import conjob.core.job.*;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.*;
import conjob.core.secret.SecretStore;
import conjob.core.secret.SecretStoreException;
import conjob.service.convert.JobResponseConverter;

import javax.ws.rs.core.Response;

public class JobService {

    private final RunJobRateLimiter runJobRateLimiter;
    private final JobConfig.LimitConfig limitConfig;
    private final JobRunCreator jobRunCreator;
    private final JobRunner jobRunner;
    private final JobRunConfigCreator jobRunConfigCreator;
    private final ResponseCreator responseCreator;
    private final JobResponseConverter jobResponseConverter;
    private final ConfigUtil configUtil;
    private final SecretStore secretStore;
    private final OutcomeDeterminer outcomeDeterminer;

    public JobService(
            DockerAdapter dockerAdapter,
            RunJobRateLimiter runJobRateLimiter, JobConfig.LimitConfig limitConfig) {
        this.runJobRateLimiter = runJobRateLimiter;
        this.limitConfig = limitConfig;

        this.secretStore = new SecretStore(dockerAdapter);
        this.jobRunCreator = new JobRunCreator(dockerAdapter);
        this.jobRunner = new JobRunner(dockerAdapter);
        this.jobRunConfigCreator = new JobRunConfigCreator();
        this.responseCreator = new ResponseCreator();
        this.jobResponseConverter = new JobResponseConverter();
        this.outcomeDeterminer = new OutcomeDeterminer();
        this.configUtil = new ConfigUtil();
    }

    public Response createResponse(String imageName) throws SecretStoreException {
        return createResponse(imageName, "");
    }

    public Response createResponse(String imageName, String input) throws SecretStoreException {
        return createResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createResponse(String imageName, String input, String pullStrategyName) throws SecretStoreException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        JobRun jobRun = runJob(imageName, input, pullStrategy);
        return createResponseFrom(jobRun);
    }

    public Response createJsonResponse(String imageName) throws SecretStoreException {
        return createJsonResponse(imageName, "");
    }

    public Response createJsonResponse(String imageName, String input) throws SecretStoreException {
        return createJsonResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createJsonResponse(String imageName, String input, String pullStrategyName) throws SecretStoreException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        JobRun jobRun = runJob(imageName, input, pullStrategy);
        return createJsonResponseFrom(jobRun);
    }

    private Response createResponseFrom(JobRun jobRun) {
        return responseCreator.create(jobRun.getConclusion())
                .entity(jobRun.getOutput())
                .build();
    }

    private Response createJsonResponseFrom(JobRun jobRun) {
        return responseCreator.create(jobRun.getConclusion())
                .entity(jobResponseConverter.from(jobRun))
                .build();
    }

    private JobRun runJob(String imageName, String input, PullStrategy pullStrategy)
            throws SecretStoreException {
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());

        if (runJobRateLimiter.isAtLimit()) {
            return new JobRun(JobRunConclusion.REJECTED, "", -1);
        }

        JobRunConfig jobRunConfig = getJobRunConfig(imageName, input);

        String jobId;
        try {
            jobId = jobRunCreator.createJob(jobRunConfig, pullStrategy);
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
