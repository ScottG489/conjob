package dci.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import dci.core.job.config.ConfigUtil;
import dci.core.job.model.Job;
import dci.core.job.model.JobResult;
import dci.core.job.model.JobRun;

public class JobService {
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient dockerClient;

    public JobService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public Job getJob(String imageName, String input, boolean shouldPull) throws DockerException, InterruptedException {
        if (shouldPull) {
            dockerClient.pull(imageName);
        }

        String secretsVolumeName = new ConfigUtil().translateToVolumeName(imageName);
        HostConfig hostConfig = getHostConfig(secretsVolumeName);
        ContainerConfig.Builder builder = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig);
        if (input != null && !input.isEmpty()) {
            builder.cmd(input);
        }
        final ContainerConfig containerConfig = builder.build();
        final ContainerCreation container = dockerClient.createContainer(containerConfig);

        dockerClient.startContainer(container.id());
//         TODO: This seems to prevent the output from being a streaming response
//        docker.stopContainer(container.id(), 600);  // 10 minutes

        LogStream logs = dockerClient.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr(), DockerClient.LogsParam.follow());
        String output = logs.readFully();

        Long exitCode = dockerClient.inspectContainer(container.id()).state().exitCode();

        return new Job(
                new JobRun(output, exitCode), JobResult.FINISHED);
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
}
