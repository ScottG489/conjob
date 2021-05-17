package conjob.service.secrets;

import conjob.core.job.config.ConfigUtil;
import conjob.core.secrets.SecretsContainerCreator;
import conjob.core.secrets.SecretsDockerAdapter;
import conjob.core.secrets.TempSecretsFileUtil;
import conjob.core.secrets.model.SecretsConfig;

import java.io.IOException;
import java.nio.file.Path;

public class SecretsService {
    // This image is required to be on the build server to create secrets
    private static final String INTERMEDIARY_CONTAINER_IMAGE = "tianon/true";
    String CONTAINER_NAME_PREFIX = "temp-container-";
    private final UniqueContainerNameGenerator uniqueContainerNameGenerator;
    String CONTAINER_DESTINATION_PATH = "/temp";

    private final SecretsDockerAdapter secretsAdapter;
    private final ConfigUtil configUtil;
    private final SecretsContainerCreator secretsContainerCreator;
    private final TempSecretsFileUtil tempSecretsFileUtil;

    public SecretsService(
            SecretsDockerAdapter secretsDockerAdapter,
            SecretsContainerCreator secretsContainerCreator,
            TempSecretsFileUtil tempSecretsFileUtil,
            UniqueContainerNameGenerator uniqueContainerNameGenerator,
            ConfigUtil configUtil) {
        this.secretsAdapter = secretsDockerAdapter;
        this.secretsContainerCreator = secretsContainerCreator;
        this.tempSecretsFileUtil = tempSecretsFileUtil;
        this.uniqueContainerNameGenerator = uniqueContainerNameGenerator;
        this.configUtil = configUtil;
    }

    public void createsSecret(String imageName, String secrets)
            throws IOException {
        String secretsVolumeName = configUtil.translateToVolumeName(imageName);
        // TODO: Could there be a race condition if two of these containers are running at the same time?
        String intermediaryContainerName =
                uniqueContainerNameGenerator.generate(CONTAINER_NAME_PREFIX);

        SecretsConfig secretsConfig =
                new SecretsConfig(
                        secretsVolumeName,
                        CONTAINER_DESTINATION_PATH,
                        INTERMEDIARY_CONTAINER_IMAGE,
                        intermediaryContainerName);
        String containerId = secretsContainerCreator.createIntermediaryContainer(secretsConfig);

        Path tempSecretsDir = tempSecretsFileUtil.createSecretsFile(secrets)
                .getParentFile().toPath();

        // TODO: It's not ideal to write the secret file to disk, if only momentarily. Refactor this to
        // TODO:   use the overloaded copyToContainer() which takes a tar stream instead.
        secretsAdapter.copySecretsToVolume(tempSecretsDir, containerId, CONTAINER_DESTINATION_PATH);

        tempSecretsFileUtil.delete(tempSecretsDir);

        secretsAdapter.removeContainer(containerId);
    }
}
