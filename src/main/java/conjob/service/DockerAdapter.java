package conjob.service;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Volume;

import java.util.List;
import java.util.stream.Collectors;

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

    public String createJobRun(JobRunConfig jobRunConfig) throws DockerException, InterruptedException {
        HostConfig hostConfig = getHostConfig(jobRunConfig.getSecretsVolumeName());

        ContainerConfig containerConfig = getContainerConfig(
                jobRunConfig.getJobName(),
                jobRunConfig.getInput(),
                hostConfig);

        return dockerClient.createContainer(containerConfig).id();
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

    public void pullImage(String imageName) throws DockerException, InterruptedException {
        dockerClient.pull(imageName);
    }

    public Long startContainerThenWaitForExit(String containerId) throws DockerException, InterruptedException {
        dockerClient.startContainer(containerId);
        return dockerClient.waitContainer(containerId).statusCode();
    }

    public Long stopContainer(String containerId, int killTimeoutSeconds) throws DockerException, InterruptedException {
        dockerClient.stopContainer(containerId, killTimeoutSeconds);
        return dockerClient.waitContainer(containerId).statusCode();
    }

    public String readAllLogsUntilExit(String containerId) throws DockerException, InterruptedException {
        LogStream logs = dockerClient.logs(
                containerId,
                DockerClient.LogsParam.stdout(),
                DockerClient.LogsParam.stderr(),
                DockerClient.LogsParam.follow());

        return logs.readFully();
    }
}
