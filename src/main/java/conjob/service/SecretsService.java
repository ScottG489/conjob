package conjob.service;

import conjob.core.job.SecretsDockerAdapter;
import conjob.core.job.config.ConfigUtil;
import conjob.core.job.exception.CreateSecretsContainerException;
import conjob.core.job.model.SecretsConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public class SecretsService {
    // This image is required to be on the build server to create secrets
    private static final String INTERMEDIARY_CONTAINER_IMAGE = "tianon/true";
    private final SecretsDockerAdapter secretsAdapter;
    private final ConfigUtil configUtil;

    public SecretsService(
            SecretsDockerAdapter secretsDockerAdapter,
            ConfigUtil configUtil) {
        this.secretsAdapter = secretsDockerAdapter;
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
        String containerId = doCreateIntermediaryContainer(secretsConfig);

        Path tempSecretsDirPath = createTempSecretsFile(secretContents).getParentFile().toPath();

        // TODO: It's not ideal to write the secret file to disk, if only momentarily. Refactor this to
        // TODO:   use the overloaded copyToContainer() which takes a tar stream instead.
        secretsAdapter.copySecretsToVolume(tempSecretsDirPath, containerId, destinationPath);

        Files.walk(tempSecretsDirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        secretsAdapter.removeContainer(containerId);
    }

    private String doCreateIntermediaryContainer(SecretsConfig secretsConfig) {
        String containerId;
        try {
            containerId = secretsAdapter.createVolumeCreatorContainer(secretsConfig);
        } catch (CreateSecretsContainerException e) {
            secretsAdapter.pullImage(secretsConfig.getIntermediaryContainerImage());
            containerId = secretsAdapter.createVolumeCreatorContainer(secretsConfig);
        }
        return containerId;
    }

    private File createTempSecretsFile(String secrets) throws IOException {
        File tempSecretsFile;
        String secretsTempDirPrefix = "secrets-temp-dir-";
        String secretsFileName = "secrets";

        File tempDirectory = getTempSecretsDir(secretsTempDirPrefix);
        tempSecretsFile = getTempSecretsFile(secretsFileName, tempDirectory);
        Files.write(tempSecretsFile.toPath(), secrets.getBytes());

        return tempSecretsFile;
    }

    private File getTempSecretsFile(String secretsFileName, File tempDirectory) throws IOException {
        java.nio.file.Path tempFilePath = Files.createFile(new File(tempDirectory, secretsFileName).toPath());
        File tempFile = tempFilePath.toFile();
        tempFile.deleteOnExit();
        return tempFile;
    }

    private File getTempSecretsDir(String secretsTempDirNamePrefix) throws IOException {
        java.nio.file.Path tempDirectory = Files.createTempDirectory(secretsTempDirNamePrefix);
        File tempDir = tempDirectory.toFile();
        tempDir.deleteOnExit();
        return tempDir;
    }
}
