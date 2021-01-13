package conjob.service;

import com.spotify.docker.client.exceptions.DockerException;
import conjob.config.JobConfig;
import conjob.core.job.RunJobRateLimiter;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.PullStrategy;
import conjob.service.convert.JobResponseConverter;

import javax.ws.rs.core.Response;
import java.util.concurrent.*;

public class JobService {
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private static final int TIMED_OUT_EXIT_CODE = -1;

    private final RunJobRateLimiter runJobRateLimiter;
    private final JobConfig.LimitConfig limitConfig;
    private final DockerAdapter dockerAdapter;

    public JobService(
            DockerAdapter dockerAdapter,
            RunJobRateLimiter runJobRateLimiter, JobConfig.LimitConfig limitConfig) {
        this.dockerAdapter = dockerAdapter;
        this.runJobRateLimiter = runJobRateLimiter;
        this.limitConfig = limitConfig;
    }

    public Response createResponse(String imageName) throws DockerException, InterruptedException, SecretStoreException {
        return createResponse(imageName, "");
    }

    public Response createResponse(String imageName, String input) throws DockerException, InterruptedException, SecretStoreException {
        return createResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createResponse(String imageName, String input, String pullStrategyName) throws DockerException, InterruptedException, SecretStoreException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        JobRun jobRun = runJob(imageName, input, pullStrategy);
        return createResponseFrom(jobRun);
    }

    public Response createJsonResponse(String imageName) throws DockerException, InterruptedException, SecretStoreException {
        return createJsonResponse(imageName, "");
    }

    public Response createJsonResponse(String imageName, String input) throws DockerException, InterruptedException, SecretStoreException {
        return createJsonResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createJsonResponse(String imageName, String input, String pullStrategyName) throws DockerException, InterruptedException, SecretStoreException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        JobRun jobRun = runJob(imageName, input, pullStrategy);
        return createJsonResponseFrom(jobRun);
    }

    private Response createResponseFrom(JobRun jobRun) {
        return new ResponseCreator().create(jobRun.getConclusion())
                .entity(jobRun.getOutput())
                .build();
    }

    private Response createJsonResponseFrom(JobRun jobRun) {
        return new ResponseCreator().create(jobRun.getConclusion())
                .entity(new JobResponseConverter().from(jobRun))
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
            jobId = new JobRunCreator(dockerAdapter).createJob(jobRunConfig, pullStrategy);
        } catch (CreateJobRunException | JobUpdateException e2) {
            runJobRateLimiter.decrementRunningJobsCount();
            return new JobRun(JobRunConclusion.NOT_FOUND, "", -1);
        }

        JobRunOutcome outcome = new JobRunner(dockerAdapter)
                .runContainer(jobId, maxTimeoutSeconds, maxKillTimeoutSeconds);
        JobRun jobRun = createJobRun(outcome);

        runJobRateLimiter.decrementRunningJobsCount();
        return jobRun;
    }

    private JobRun createJobRun(JobRunOutcome outcome) {
        JobRunConclusion jobRunConclusion;
        if (outcome.getExitStatusCode() == TIMED_OUT_EXIT_CODE) {
            jobRunConclusion = JobRunConclusion.TIMED_OUT;
        } else if (outcome.getExitStatusCode() != 0) {
            jobRunConclusion = JobRunConclusion.FAILURE;
        } else {
            jobRunConclusion = JobRunConclusion.SUCCESS;
        }

        return new JobRun(jobRunConclusion, outcome.getOutput(), outcome.getExitStatusCode());
    }

    private JobRunConfig getJobRunConfig(String imageName, String input) throws SecretStoreException {
        String correspondingSecretsVolumeName = new ConfigUtil().translateToVolumeName(imageName);
        String secretId = new SecretStore(dockerAdapter)
                .findSecret(correspondingSecretsVolumeName)
                .orElse(null);

        return new JobRunConfigCreator().getContainerConfig(imageName, input, secretId);
    }
}
