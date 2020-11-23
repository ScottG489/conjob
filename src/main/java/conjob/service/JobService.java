package conjob.service;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.exceptions.ImagePullFailedException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import conjob.config.JobConfig;
import conjob.core.job.LogsAdapter;
import conjob.core.job.RunJobRateLimiter;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.model.Job;
import conjob.core.job.model.JobResult;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.PullStrategy;
import conjob.service.convert.JobResponseAugmenter;
import conjob.service.convert.JobResponseConverter;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.*;

public class JobService {
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient dockerClient;
    private final RunJobRateLimiter runJobRateLimiter;
    private final JobConfig.LimitConfig limitConfig;

    public JobService(
            DockerClient dockerClient,
            RunJobRateLimiter runJobRateLimiter, JobConfig.LimitConfig limitConfig) {
        this.dockerClient = dockerClient;
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
        Job job = runJob(imageName, input, pullStrategy);
        return createResponseFrom(job);
    }

    public Response createJsonResponse(String imageName) throws DockerException, InterruptedException {
        return createJsonResponse(imageName, "");
    }

    public Response createJsonResponse(String imageName, String input) throws DockerException, InterruptedException {
        return createJsonResponse(imageName, input, PullStrategy.ALWAYS.name());
    }

    public Response createJsonResponse(String imageName, String input, String pullStrategyName) throws DockerException, InterruptedException {
        PullStrategy pullStrategy = PullStrategy.valueOf(pullStrategyName.toUpperCase());
        Job job = runJob(imageName, input, pullStrategy);
        return createJsonResponseFrom(job);
    }

    private Job runJob(String imageName, String input, PullStrategy pullStrategy) throws DockerException, InterruptedException {
        long maxTimeoutSeconds = limitConfig.getMaxTimeoutSeconds();
        int maxKillTimeoutSeconds = Math.toIntExact(limitConfig.getMaxKillTimeoutSeconds());

        if (runJobRateLimiter.isAtLimit()) {
            return new Job(new JobRun("", -1), JobResult.REJECTED);
        }

        final ContainerConfig containerConfig = getContainerConfig(imageName, input);
        final Optional<ContainerCreation> containerTry =
                tryContainerCreate(containerConfig, pullStrategy);
        if (containerTry.isEmpty()) {
            runJobRateLimiter.decrementRunningJobsCount();
            return new Job(new JobRun("", -1), JobResult.NOT_FOUND);
        }

        final ContainerCreation container = containerTry.get();

        dockerClient.startContainer(container.id());
        Long exitCode = waitForJob(dockerClient, container.id(), maxTimeoutSeconds, maxKillTimeoutSeconds);

        String output = new LogsAdapter(dockerClient).readAllLogsUntilExit(container.id());

        JobResult jobResult;
        int SIGKILL = 137;
        int SIGTERM = 143;
        if (exitCode == SIGKILL || exitCode == SIGTERM) {
            jobResult = JobResult.KILLED;
        } else {
            jobResult = JobResult.FINISHED;
        }

        runJobRateLimiter.decrementRunningJobsCount();
        return new Job(new JobRun(output, exitCode), jobResult);
    }

    private Response createResponseFrom(Job job) {
        return new JobResponseAugmenter().create(job)
                .entity(job.getJobRun().getOutput())
                .build();
    }

    private Response createJsonResponseFrom(Job job) {
        return new JobResponseAugmenter().create(job)
                .entity(new JobResponseConverter().from(job))
                .build();
    }

    private Long waitForJob(DockerClient dockerClient, String containerId, long timeoutSeconds, int killTimeoutSeconds) throws InterruptedException, DockerException {
        Long exitStatusCode;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Long> future = executor.submit(new WaitForContainer(dockerClient, containerId));
        try {
            exitStatusCode = future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ignored) {
            dockerClient.stopContainer(containerId, killTimeoutSeconds);
            // The container could finish naturally before the job timeout but before the stop-to-kill timeout.
            exitStatusCode = dockerClient.waitContainer(containerId).statusCode();
        }
        executor.shutdownNow();
        return exitStatusCode;
    }

    private ContainerConfig getContainerConfig(String imageName, String input) throws DockerException, InterruptedException {
        String secretsVolumeName = new ConfigUtil().translateToVolumeName(imageName);
        HostConfig hostConfig = getHostConfig(secretsVolumeName);
        ContainerConfig.Builder builder = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig);
        if (input != null && !input.isEmpty()) {
            builder.cmd(input);
        }
        return builder.build();
    }

    private HostConfig getHostConfig(String secretsVolumeName) throws DockerException, InterruptedException {
        HostConfig.Builder builder = HostConfig.builder()
                .runtime("sysbox-runc");

        dockerClient.listVolumes().volumes().stream()
                .filter(volume -> volume.name().equals(secretsVolumeName))
                .limit(1)
                .forEach(volume -> builder.appendBinds(secretsVolumeName + ":" + SECRETS_VOLUME_MOUNT_PATH + ":" + SECRETS_VOLUME_MOUNT_OPTIONS));

        return builder.build();
    }

    // ContainerCreator (class) | ContainerCreator.PullStrategy (enum)
    private Optional<ContainerCreation> tryContainerCreate(ContainerConfig containerConfig, PullStrategy pullStrategy)
            throws DockerException, InterruptedException {
        Optional<ContainerCreation> container;

        switch (pullStrategy) {
            case NEVER:
                try {
                    container = Optional.of(dockerClient.createContainer(containerConfig));
                } catch (ImageNotFoundException e) {
                    container = Optional.empty();
                }
                break;
            case ALWAYS:
                try {
                    dockerClient.pull(containerConfig.image());
                    container = Optional.of(dockerClient.createContainer(containerConfig));
                } catch (ImageNotFoundException | ImagePullFailedException e2) {
                    try {
                        // The pull will fail if no tag is specified but it's still pulled so we can run it
                        container = Optional.of(dockerClient.createContainer(containerConfig));
                    } catch (ImageNotFoundException | ImagePullFailedException e3) {
                        container = Optional.empty();
                    }
                }
                break;
            case ABSENT:
                try {
                    container = Optional.of(dockerClient.createContainer(containerConfig));
                } catch (ImageNotFoundException e) {
                    try {
                        dockerClient.pull(containerConfig.image());
                        container = Optional.of(dockerClient.createContainer(containerConfig));
                    } catch (ImageNotFoundException | ImagePullFailedException e2) {
                        try {
                            // The pull will fail if no tag is specified but it's still pulled so we can run it
                            container = Optional.of(dockerClient.createContainer(containerConfig));
                       } catch (ImageNotFoundException | ImagePullFailedException e3) {
                            container = Optional.empty();
                        }
                    }
                }
                break;
            default:
                container = Optional.empty();
        }

        return container;
    }

    static class WaitForContainer implements Callable<Long> {
        private final DockerClient dockerClient;
        private final String containerId;

        public WaitForContainer(DockerClient dockerClient, String containerId) {
            this.dockerClient = dockerClient;
            this.containerId = containerId;
        }

        @Override
        public Long call() throws DockerException, InterruptedException {
            return dockerClient.waitContainer(containerId).statusCode();
        }
    }
}
