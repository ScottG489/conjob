package conjob.service;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import conjob.config.JobConfig;
import conjob.core.job.RunJobRateLimiter;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.PullStrategy;
import conjob.service.convert.JobResponseConverter;

import javax.ws.rs.core.Response;
import java.util.Optional;
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

    public Response createResponse(String imageName) throws DockerException, InterruptedException {
        return createResponse(imageName, "");
    }

    public Response createResponse(String imageName, String input) throws DockerException, InterruptedException {
        return createResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createResponse(String imageName, String input, String pullStrategyName) throws DockerException, InterruptedException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        JobRun jobRun = runJob(imageName, input, pullStrategy);
        return createResponseFrom(jobRun);
    }

    public Response createJsonResponse(String imageName) throws DockerException, InterruptedException {
        return createJsonResponse(imageName, "");
    }

    public Response createJsonResponse(String imageName, String input) throws DockerException, InterruptedException {
        return createJsonResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createJsonResponse(String imageName, String input, String pullStrategyName) throws DockerException, InterruptedException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        JobRun jobRun = runJob(imageName, input, pullStrategy);
        return createJsonResponseFrom(jobRun);
    }

    private JobRun runJob(String imageName, String input, PullStrategy pullStrategy) throws DockerException, InterruptedException {
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());

        if (runJobRateLimiter.isAtLimit()) {
            return new JobRun(JobRunConclusion.REJECTED, "", -1);
        }

        HostConfig hostConfig = getHostConfig(imageName);
        ContainerConfig containerConfig = getContainerConfig(imageName, input, hostConfig);

        ContainerCreation container;
        try {
            container = createContainer(containerConfig, pullStrategy);
        } catch (ImageNotFoundException e2) {
            runJobRateLimiter.decrementRunningJobsCount();
            return new JobRun(JobRunConclusion.NOT_FOUND, "", -1);
        }

        Long exitCode = runContainer(container.id(), maxTimeoutSeconds, maxKillTimeoutSeconds);

        String output = dockerAdapter.readAllLogsUntilExit(container.id());

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

    private ContainerConfig getContainerConfig(String imageName, String input, HostConfig hostConfig) {
        ContainerConfig containerConfig;
        if (input != null && !input.isEmpty()) {
            containerConfig = dockerAdapter.createContainerConfigWithInput(imageName, hostConfig, input);
        } else {
            containerConfig = dockerAdapter.createContainerConfig(imageName, hostConfig);
        }
        return containerConfig;
    }

    private HostConfig getHostConfig(String imageName) throws DockerException, InterruptedException {
        String secretsVolumeName = new ConfigUtil().translateToVolumeName(imageName);
        HostConfig hostConfig;
        String runtime = "sysbox-runc";

        Optional<String> existingSecretsVolume = dockerAdapter.listAllVolumeNames().stream()
                .filter(volName -> volName.equals(secretsVolumeName))
                .findFirst();

        if (existingSecretsVolume.isPresent()) {
            hostConfig = dockerAdapter.createHostConfigWithBind(
                    runtime,
                    existingSecretsVolume.get()
                            + ":" + SECRETS_VOLUME_MOUNT_PATH
                            + ":" + SECRETS_VOLUME_MOUNT_OPTIONS);
        } else {
            hostConfig = dockerAdapter.createHostConfig(runtime);
        }

        return hostConfig;
    }

    // ContainerCreator (class) | ContainerCreator.PullStrategy (enum)
    private ContainerCreation createContainer(ContainerConfig containerConfig, PullStrategy pullStrategy)
            throws DockerException, InterruptedException {
        ContainerCreation container;

        switch (pullStrategy) {
            case NEVER:
                container = dockerAdapter.createContainer(containerConfig);
                break;
            case ALWAYS:
                container = dockerAdapter.pullThenCreateContainer(containerConfig);
                break;
            case ABSENT:
                try {
                    container = dockerAdapter.createContainer(containerConfig);
                } catch (ImageNotFoundException e) {
                    container = dockerAdapter.pullThenCreateContainer(containerConfig);
                }
                break;
            default:
                throw new RuntimeException("Unknown pull strategy: " + pullStrategy.name());
        }

        return container;
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
