package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.exceptions.ImagePullFailedException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.model.Job;
import conjob.core.job.model.JobResult;
import conjob.core.job.model.JobRun;

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

    public Job getJob(String imageName, String input) throws DockerException, InterruptedException {
        if (runJobRateLimiter.isAtLimit()) {
            return new Job(
                    new JobRun("", -1), JobResult.REJECTED);
        }

        final ContainerConfig containerConfig = getContainerConfig(imageName, input);
        final Optional<ContainerCreation> containerTry = tryContainerCreate(containerConfig);
        if (!containerTry.isPresent()) {
            runJobRateLimiter.decrementRunningJobsCount();
            return new Job(
                    new JobRun("", -1), JobResult.NOT_FOUND);
        }
        final ContainerCreation container = containerTry.get();

        dockerClient.startContainer(container.id());
        dockerClient.waitContainer(container.id());

        LogStream logs = dockerClient.logs(
                container.id(),
                DockerClient.LogsParam.stdout(),
                DockerClient.LogsParam.stderr(),
                DockerClient.LogsParam.follow());

        String output = waitForJobRun(container.id(), logs);

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
        return new Job(
                new JobRun(output, exitCode), jobResult);
    }

    private String waitForJobRun(String containerId, LogStream logs) throws InterruptedException, DockerException {
        String output = "";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new ReadLogsFullyCallable(logs));
        try {
            output = future.get(30, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException ignored) {
            dockerClient.stopContainer(containerId, 60);
        }
        executor.shutdownNow();
        return output;
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

    private Optional<ContainerCreation> tryContainerCreate(ContainerConfig containerConfig) throws DockerException, InterruptedException {
        Optional<ContainerCreation> container;

        try {
            container = Optional.of(dockerClient.createContainer(containerConfig));
        } catch (ImageNotFoundException e) {
            try {
                dockerClient.pull(containerConfig.image());
                container = Optional.of(dockerClient.createContainer(containerConfig));
            } catch (ImageNotFoundException | ImagePullFailedException e2) {
                try {
                    container = Optional.of(dockerClient.createContainer(containerConfig));
                } catch (ImageNotFoundException | ImagePullFailedException e3) {
                    container = Optional.empty();
                }
            }
        }

        return container;
    }

    static class ReadLogsFullyCallable implements Callable<String> {
        private final LogStream logStream;

        ReadLogsFullyCallable(LogStream logStream) {
            this.logStream = logStream;
        }

        @Override
        public String call() {
            return logStream.readFully();
        }
    }
}
