package conjob.service;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.exceptions.ImagePullFailedException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import conjob.core.job.RunJobRateLimiter;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.model.Job;
import conjob.core.job.model.JobResult;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.PullStrategy;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.*;

public class JobService {
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient dockerClient;
    private final RunJobRateLimiter runJobRateLimiter;

    public JobService(
            DockerClient dockerClient,
            RunJobRateLimiter runJobRateLimiter) {
        this.dockerClient = dockerClient;
        this.runJobRateLimiter = runJobRateLimiter;
    }

    public Response createResponse(String imageName, String input, String pullStrategy) throws DockerException, InterruptedException {
        if (runJobRateLimiter.isAtLimit()) {
            Job job = new Job(new JobRun("", -1), JobResult.REJECTED);
            return createResponseFrom(job);
        }

        final ContainerConfig containerConfig = getContainerConfig(imageName, input);
        final Optional<ContainerCreation> containerTry =
                tryContainerCreate(containerConfig, PullStrategy.valueOf(pullStrategy.toUpperCase()));
        if (containerTry.isEmpty()) {
            runJobRateLimiter.decrementRunningJobsCount();
            Job job = new Job(new JobRun("", -1), JobResult.NOT_FOUND);
            return createResponseFrom(job);
        }
        final ContainerCreation container = containerTry.get();

        dockerClient.startContainer(container.id());
        waitForJob(dockerClient, container.id());

        LogStream logs = dockerClient.logs(
                container.id(),
                DockerClient.LogsParam.stdout(),
                DockerClient.LogsParam.stderr(),
                DockerClient.LogsParam.follow());

        String output = logs.readFully();

        // TODO: Use exit code from waitContainer instead
        Long exitCode = dockerClient.inspectContainer(container.id()).state().exitCode();

        JobResult jobResult;
        int SIGKILL = 137;
        int SIGTERM = 143;
        if (exitCode == SIGKILL || exitCode == SIGTERM) {
            jobResult = JobResult.KILLED;
        } else {
            jobResult = JobResult.FINISHED;
        }
        runJobRateLimiter.decrementRunningJobsCount();
        Job job = new Job(new JobRun(output, exitCode), jobResult);
        return createResponseFrom(job);
    }

    public Response createJsonResponse(String imageName, String input, String pullStrategy) throws DockerException, InterruptedException {
        if (runJobRateLimiter.isAtLimit()) {
            Job job = new Job(new JobRun("", -1), JobResult.REJECTED);
            return createJsonResponseFrom(job);
        }

        final ContainerConfig containerConfig = getContainerConfig(imageName, input);
        final Optional<ContainerCreation> containerTry =
                tryContainerCreate(containerConfig, PullStrategy.valueOf(pullStrategy.toUpperCase()));
        if (containerTry.isEmpty()) {
            runJobRateLimiter.decrementRunningJobsCount();
            Job job = new Job(new JobRun("", -1), JobResult.NOT_FOUND);
            return createJsonResponseFrom(job);
        }
        final ContainerCreation container = containerTry.get();

        dockerClient.startContainer(container.id());
        waitForJob(dockerClient, container.id());

        LogStream logs = dockerClient.logs(
                container.id(),
                DockerClient.LogsParam.stdout(),
                DockerClient.LogsParam.stderr(),
                DockerClient.LogsParam.follow());

        String output = logs.readFully();

        // TODO: Use exit code from waitContainer instead
        Long exitCode = dockerClient.inspectContainer(container.id()).state().exitCode();

        JobResult jobResult;
        int SIGKILL = 137;
        int SIGTERM = 143;
        if (exitCode == SIGKILL || exitCode == SIGTERM) {
            jobResult = JobResult.KILLED;
        } else {
            jobResult = JobResult.FINISHED;
        }
        runJobRateLimiter.decrementRunningJobsCount();
        Job job = new Job(new JobRun(output, exitCode), jobResult);
        return createJsonResponseFrom(job);
    }

    private Response createResponseFrom(Job job) {
        return createResponseWithStatus(job)
                .entity(job.getJobRun().getOutput())
                .build();
    }

    private Response createJsonResponseFrom(Job job) {
        return createResponseWithStatus(job)
                .entity(new JobResponseConverter().from(job))
                .build();
    }

    private Long waitForJob(DockerClient dockerClient, String containerId) throws InterruptedException, DockerException {
        Long exitStatusCode;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Long> future = executor.submit(new WaitForContainer(dockerClient, containerId));
        try {
            exitStatusCode = future.get(30, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException ignored) {
            dockerClient.stopContainer(containerId, 60);
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

    private Response.ResponseBuilder createResponseWithStatus(Job job) {
        Response.ResponseBuilder responseBuilder;
        JobResult jobResult = job.getResult();
        long exitCode = job.getJobRun().getExitCode();

        if (jobResult.equals(JobResult.FINISHED)) {
            if (exitCode == 0) {
                responseBuilder = Response.ok();
            } else {
                responseBuilder = Response.status(Response.Status.BAD_REQUEST);
            }
        } else if (jobResult.equals(JobResult.NOT_FOUND)) {
            responseBuilder = Response.status(Response.Status.NOT_FOUND);
        } else if (jobResult.equals(JobResult.KILLED)) {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        } else if (jobResult.equals(JobResult.REJECTED)) {
            responseBuilder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
        } else {
            responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return responseBuilder;
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
