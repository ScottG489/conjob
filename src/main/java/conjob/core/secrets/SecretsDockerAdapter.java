package conjob.core.secrets;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;
import conjob.core.secrets.exception.CopySecretsToContainerException;
import conjob.core.secrets.exception.CreateSecretsContainerException;
import conjob.core.secrets.exception.RemoveSecretsContainerException;
import conjob.core.secrets.exception.UpdateSecretsImageException;
import conjob.core.secrets.model.SecretsConfig;

import java.io.IOException;
import java.nio.file.Path;

public class SecretsDockerAdapter {
    private final DockerClient dockerClient;

    public SecretsDockerAdapter(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createVolumeCreatorContainer(SecretsConfig secretsConfig) {
        HostConfig hostConfig = HostConfig.builder()
                .binds(secretsConfig.getSecretsVolumeName() + ":" + secretsConfig.getDestinationPath())
                .build();

        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(secretsConfig.getIntermediaryContainerImage())
                .build();

        try {
            return dockerClient.createContainer(
                    containerConfig, secretsConfig.getIntermediaryContainerName())
                    .id();
        } catch (DockerException | InterruptedException e) {
            throw new CreateSecretsContainerException(e);
        }
    }

    public void pullImage(String imageName) {
        try {
            dockerClient.pull(imageName);
        } catch (DockerException | InterruptedException e) {
            throw new UpdateSecretsImageException(e);
        }
    }

    public void copySecretsToVolume(Path sourceSecretsFile, String containerId, String destinationPath) {
        try {
            dockerClient.copyToContainer(
                    sourceSecretsFile,
                    containerId,
                    destinationPath);
        } catch (DockerException | InterruptedException | IOException e) {
            throw new CopySecretsToContainerException(e);
        }
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainer(containerId);
        } catch (DockerException | InterruptedException e) {
            throw new RemoveSecretsContainerException(e);
        }
    }
}
