package conjob.core.secrets;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import conjob.core.secrets.exception.CopySecretsToContainerException;
import conjob.core.secrets.exception.CreateSecretsContainerException;
import conjob.core.secrets.exception.RemoveSecretsContainerException;
import conjob.core.secrets.exception.UpdateSecretsImageException;
import conjob.core.secrets.model.SecretsConfig;

import java.nio.file.Path;

public class SecretsDockerAdapter {
    private final DockerClient dockerClient;

    public SecretsDockerAdapter(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createVolumeCreatorContainer(SecretsConfig secretsConfig) {
        Bind bind = new Bind(secretsConfig.getSecretsVolumeName(),
                new Volume(secretsConfig.getDestinationPath()));
        HostConfig hostConfig = new HostConfig().withBinds(bind);

        try {
            CreateContainerResponse response = dockerClient.createContainerCmd(secretsConfig.getIntermediaryContainerImage())
                    .withName(secretsConfig.getIntermediaryContainerName())
                    .withHostConfig(hostConfig)
                    .exec();
            return response.getId();
        } catch (Exception e) {
            throw new CreateSecretsContainerException(e);
        }
    }

    public void pullImage(String imageName) {
        try {
            dockerClient.pullImageCmd(imageName).start().awaitCompletion();
        } catch (Exception e) {
            throw new UpdateSecretsImageException(e);
        }
    }

    public void copySecretsToVolume(Path sourceSecretsFile, String containerId, String destinationPath) {
        try {
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withHostResource(sourceSecretsFile.toString())
                    .withRemotePath(destinationPath)
                    .exec();
        } catch (Exception e) {
            throw new CopySecretsToContainerException(e);
        }
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            throw new RemoveSecretsContainerException(e);
        }
    }
}
