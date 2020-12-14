package conjob.service;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Volume;

import java.util.List;
import java.util.stream.Collectors;

public class DockerAdapter {
    private final DockerClient dockerClient;

    public DockerAdapter(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public List<String> listAllVolumeNames() throws DockerException, InterruptedException {
        return dockerClient.listVolumes().volumes().stream()
                .map(Volume::name).collect(Collectors.toList());
    }

    public HostConfig createHostConfig(String runtime) {
        return HostConfig.builder().runtime(runtime).build();
    }

    public HostConfig createHostConfigWithBind(String runtime, String bind) {
        return HostConfig.builder()
                .runtime(runtime)
                .appendBinds(bind).build();
    }

    public ContainerConfig createContainerConfig(String imageName, HostConfig hostConfig) {
        return ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig).build();
    }

    public ContainerConfig createContainerConfigWithInput(String imageName, HostConfig hostConfig, String input) {
        return ContainerConfig.builder()
                .image(imageName)
                .cmd(input)
                .hostConfig(hostConfig).build();
    }

    public ContainerCreation createContainer(ContainerConfig containerConfig) throws DockerException, InterruptedException {
        return dockerClient.createContainer(containerConfig);
    }

    public ContainerCreation pullThenCreateContainer(ContainerConfig containerConfig) throws DockerException, InterruptedException {
        dockerClient.pull(containerConfig.image());
        return dockerClient.createContainer(containerConfig);
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
