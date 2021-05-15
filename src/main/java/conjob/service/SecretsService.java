package conjob.service;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.exceptions.ImagePullFailedException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import conjob.core.job.config.ConfigUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class SecretsService {
    // This image is required to be on the build server to create secrets
    private static final String INTERMEDIARY_CONTAINER_IMAGE = "tianon/true";
    private final DockerClient docker;
    private final ConfigUtil configUtil;

    public SecretsService(DockerClient docker, ConfigUtil configUtil) throws DockerException, InterruptedException {
        this.docker = docker;
        this.configUtil = configUtil;
        if (docker.listImages(DockerClient.ListImagesParam.byName(INTERMEDIARY_CONTAINER_IMAGE)).isEmpty()) {
            docker.pull(INTERMEDIARY_CONTAINER_IMAGE);
        }
    }

    public void createSecret(String imageName,
                             String input)
            throws DockerException, InterruptedException, IOException {
        String secretsVolumeName = configUtil.translateToVolumeName(imageName);
        // TODO: Could there be a race condition if two of these containers are running at the same time?
        String intermediaryContainerName = "temp-container-" + UUID.randomUUID().toString();
        String destinationPath = "/temp";

        ContainerConfig containerConfig = getContainerConfig(secretsVolumeName, destinationPath);
        ContainerCreation container = createIntermediaryContainer(intermediaryContainerName, containerConfig, docker);

        String secretsTempDir = "secrets-temp-dir";
        String secretsFileName = "secrets";

        File tempDirectory = getTempSecretsDir(secretsTempDir);
        File tempFile = getTempSecretsFile(secretsFileName, tempDirectory);
        Files.write(new File(tempDirectory, tempFile.getName()).toPath(), input.getBytes());
        docker.copyToContainer(tempDirectory.toPath(), container.id(), destinationPath);

        tempFile.delete();
        tempDirectory.delete();
        docker.removeContainer(container.id());
    }

    private ContainerConfig getContainerConfig(String secretsVolumeName, String destinationPath) {
        HostConfig hostConfig = HostConfig.builder().binds(secretsVolumeName + ":" + destinationPath).build();

        return ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(SecretsService.INTERMEDIARY_CONTAINER_IMAGE)
                .build();
    }

    private ContainerCreation createIntermediaryContainer(
            String intermediaryContainerName,
            ContainerConfig containerConfig,
            DockerClient dockerClient) throws DockerException, InterruptedException {
        ContainerCreation container;
        // Pull-if-absent logic
        try {
            container = dockerClient.createContainer(containerConfig, intermediaryContainerName);
        } catch (ImageNotFoundException e) {
            try {
                dockerClient.pull(containerConfig.image());
                container = dockerClient.createContainer(containerConfig, intermediaryContainerName);
            } catch (ImageNotFoundException | ImagePullFailedException e2) {
                // The pull will fail if no tag is specified but it's still pulled so we can run it
                container = dockerClient.createContainer(containerConfig, intermediaryContainerName);
            }
        }
        return container;
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