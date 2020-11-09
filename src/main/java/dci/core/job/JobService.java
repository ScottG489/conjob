package dci.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.exceptions.ImagePullFailedException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import dci.core.job.config.ConfigUtil;
import dci.core.job.model.Job;
import dci.core.job.model.JobResult;
import dci.core.job.model.JobRun;

import java.util.Optional;

public class JobService {
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient dockerClient;

    public JobService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public Job getJob(String imageName, String input) throws DockerException, InterruptedException {
        final ContainerConfig containerConfig = getContainerConfig(imageName, input);
        final Optional<ContainerCreation> containerTry = tryContainerCreate(containerConfig);
        if (!containerTry.isPresent()) {
            return new Job(
                    new JobRun("", -1), JobResult.NOT_FOUND);
        }
        final ContainerCreation container = containerTry.get();

        dockerClient.startContainer(container.id());
//         TODO: This seems to prevent the output from being a streaming response
//        docker.stopContainer(container.id(), 600);  // 10 minutes

        LogStream logs = dockerClient.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr(), DockerClient.LogsParam.follow());
        String output = logs.readFully();

        Long exitCode = dockerClient.inspectContainer(container.id()).state().exitCode();

        return new Job(
                new JobRun(output, exitCode), JobResult.FINISHED);
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
                .appendBinds("/var/run/docker.sock:/var/run/docker.sock");

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
}
