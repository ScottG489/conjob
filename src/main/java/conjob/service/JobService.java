package conjob.service;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
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

    private JobRun runJob(String imageName, String input, PullStrategy pullStrategy) throws DockerException, InterruptedException, SecretStoreException {
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());

        if (runJobRateLimiter.isAtLimit()) {
            return new JobRun(JobRunConclusion.REJECTED, "", -1);
        }

        String correspondingSecretsVolumeName = new ConfigUtil().translateToVolumeName(imageName);
        String secretId = new SecretStore(dockerAdapter)
                .findSecret(correspondingSecretsVolumeName)
                .orElse(null);

        JobRunConfig jobRunConfig = new JobRunConfigCreator().getContainerConfig(imageName, input, secretId);

        String jobId;
        try {
            jobId = createJob(jobRunConfig, pullStrategy);
        } catch (ImageNotFoundException e2) {
            runJobRateLimiter.decrementRunningJobsCount();
            return new JobRun(JobRunConclusion.NOT_FOUND, "", -1);
        }

        Long exitCode = runContainer(jobId, maxTimeoutSeconds, maxKillTimeoutSeconds);

        String output = dockerAdapter.readAllLogsUntilExit(jobId);

        JobRunConclusion jobRunConclusion;
        if (exitCode == TIMED_OUT_EXIT_CODE) {
            jobRunConclusion = JobRunConclusion.TIMED_OUT;
        } else if (exitCode != 0) {
            jobRunConclusion = JobRunConclusion.FAILURE;
        } else {
            jobRunConclusion = JobRunConclusion.SUCCESS;
        }

        runJobRateLimiter.decrementRunningJobsCount();
        return new JobRun(jobRunConclusion, output, exitCode);
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

    private Long runContainer(String containerId, long timeoutSeconds, int killTimeoutSeconds) throws InterruptedException, DockerException {
        Long exitStatusCode;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Long> future = executor.submit(new WaitForContainer(dockerAdapter, containerId));
        try {
            exitStatusCode = future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ignored) {
            exitStatusCode = dockerAdapter.stopContainer(containerId, killTimeoutSeconds);
            // The container could finish naturally before the job timeout but before the stop-to-kill timeout.
            exitStatusCode = wasStoppedOrKilled(exitStatusCode) ? -1 : 0L;
        }
        executor.shutdownNow();
        return exitStatusCode;
    }

    private boolean wasStoppedOrKilled(Long exitCode) {
        final int SIGKILL = 137;
        final int SIGTERM = 143;
        return exitCode == SIGKILL || exitCode == SIGTERM;
    }

    // ContainerCreator (class) | ContainerCreator.PullStrategy (enum)
    private String createJob(JobRunConfig jobRunConfig, PullStrategy pullStrategy)
            throws DockerException, InterruptedException {
        String jobId;

        switch (pullStrategy) {
            case NEVER:
                jobId = dockerAdapter.createJobRun(jobRunConfig);
                break;
            case ALWAYS:
                dockerAdapter.pullImage(jobRunConfig.getJobName());
                jobId = dockerAdapter.createJobRun(jobRunConfig);
                break;
            case ABSENT:
                try {
                    jobId = dockerAdapter.createJobRun(jobRunConfig);
                } catch (ImageNotFoundException e) {
                    dockerAdapter.pullImage(jobRunConfig.getJobName());
                    jobId = dockerAdapter.createJobRun(jobRunConfig);
                }
                break;
            default:
                throw new RuntimeException("Unknown pull strategy: " + pullStrategy.name());
        }

        return jobId;
    }

    static class WaitForContainer implements Callable<Long> {
        private final DockerAdapter dockerAdapter;
        private final String containerId;

        public WaitForContainer(DockerAdapter dockerClient, String containerId) {
            this.dockerAdapter = dockerClient;
            this.containerId = containerId;
        }

        @Override
        public Long call() throws DockerException, InterruptedException {
            return dockerAdapter.startContainerThenWaitForExit(containerId);
        }
    }
}
