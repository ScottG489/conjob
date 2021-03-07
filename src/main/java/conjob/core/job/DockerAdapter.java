package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Volume;
import conjob.core.job.exception.*;

import java.util.List;
import java.util.stream.Collectors;

// TODO: Create more specific exceptions for when ImageNotFoundException is thrown
public class DockerAdapter {
    private static final String RUNTIME = "sysbox-runc";
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient dockerClient;

    public DockerAdapter(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public List<String> listAllVolumeNames() throws DockerException, InterruptedException {
        return dockerClient.listVolumes().volumes().stream()
                .map(Volume::name).collect(Collectors.toList());
    }

    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        HostConfig hostConfig = getHostConfig(jobRunConfig.getSecretsVolumeName());

        ContainerConfig containerConfig = getContainerConfig(
                jobRunConfig.getJobName(),
                jobRunConfig.getInput(),
                hostConfig);

        try {
            return dockerClient.createContainer(containerConfig).id();
        } catch (DockerException | InterruptedException e) {
            throw new CreateJobRunException(e);
        }
    }

    private ContainerConfig getContainerConfig(String jobName, String input, HostConfig hostConfig) {
        ContainerConfig.Builder containerConfigBuilder = ContainerConfig.builder()
                .image(jobName)
                .hostConfig(hostConfig);

        if (input != null) {
            containerConfigBuilder.cmd(input);
        }

        return containerConfigBuilder.build();
    }

    private HostConfig getHostConfig(String secretsVolumeName) {
        HostConfig.Builder hostConfigBuilder = HostConfig.builder().runtime(RUNTIME);
        if (secretsVolumeName != null) {
            hostConfigBuilder.appendBinds(
                    secretsVolumeName
                            + ":" + SECRETS_VOLUME_MOUNT_PATH
                            + ":" + SECRETS_VOLUME_MOUNT_OPTIONS);
        }
        return hostConfigBuilder.build();
    }

    public void pullImage(String imageName) throws JobUpdateException {
        try {
            dockerClient.pull(imageName);
        } catch (InterruptedException | DockerException e2) {
            throw new JobUpdateException(e2);
        }
    }

    public Long startContainerThenWaitForExit(String containerId) throws RunJobException {
        try {
            dockerClient.startContainer(containerId);
            return dockerClient.waitContainer(containerId).statusCode();
        } catch (DockerException | InterruptedException e) {
            throw new RunJobException(e);
        }
    }

    public Long stopContainer(String containerId, int killTimeoutSeconds) throws StopJobRunException {
        try {
            dockerClient.stopContainer(containerId, killTimeoutSeconds);
            return dockerClient.waitContainer(containerId).statusCode();
        } catch (DockerException | InterruptedException e) {
            throw new StopJobRunException(e);
        }
    }

    public String readAllLogsUntilExit(String containerId) throws ReadLogsException {
        LogStream logs;
        try {
            logs = dockerClient.logs(
                    containerId,
                    DockerClient.LogsParam.stdout(),
                    DockerClient.LogsParam.stderr(),
                    DockerClient.LogsParam.follow());
            return logs.readFully();
        } catch (DockerException | InterruptedException e) {
            throw new ReadLogsException(e);
        }
    }
}
