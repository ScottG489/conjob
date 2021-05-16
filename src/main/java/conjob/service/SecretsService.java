package conjob.service;

import conjob.core.job.SecretsDockerAdapter;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.model.SecretsConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class SecretsService {
    // This image is required to be on the build server to create secrets
    private static final String INTERMEDIARY_CONTAINER_IMAGE = "tianon/true";
    private final SecretsDockerAdapter secretsAdapter;
    private final ConfigUtil configUtil;
    private final SecretsContainerCreator secretsContainerCreator;
    private final TempSecretsFileUtil tempSecretsFileUtil;

    public SecretsService(
            SecretsDockerAdapter secretsDockerAdapter,
            SecretsContainerCreator secretsContainerCreator,
            TempSecretsFileUtil tempSecretsFileUtil,
            ConfigUtil configUtil) {
        this.secretsAdapter = secretsDockerAdapter;
        this.secretsContainerCreator = secretsContainerCreator;
        this.tempSecretsFileUtil = tempSecretsFileUtil;
        this.configUtil = configUtil;
    }

    public void createSecret(String imageName, String secretContents)
            throws IOException {
        String secretsVolumeName = configUtil.translateToVolumeName(imageName);
        // TODO: Could there be a race condition if two of these containers are running at the same time?
        String intermediaryContainerName = "temp-container-" + UUID.randomUUID().toString();
        String destinationPath = "/temp";

        SecretsConfig secretsConfig =
                new SecretsConfig(
                        secretsVolumeName,
                        destinationPath,
                        INTERMEDIARY_CONTAINER_IMAGE,
                        intermediaryContainerName);
        String containerId = secretsContainerCreator.createIntermediaryContainer(secretsConfig);

        Path tempSecretsDirPath = tempSecretsFileUtil.createSecretsFile(secretContents)
                .getParentFile().toPath();

        // TODO: It's not ideal to write the secret file to disk, if only momentarily. Refactor this to
        // TODO:   use the overloaded copyToContainer() which takes a tar stream instead.
        secretsAdapter.copySecretsToVolume(tempSecretsDirPath, containerId, destinationPath);

        tempSecretsFileUtil.delete(tempSecretsDirPath);

        secretsAdapter.removeContainer(containerId);
    }
}
