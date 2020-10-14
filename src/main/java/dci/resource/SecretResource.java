package dci.resource;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Path("/secret")
@Slf4j
public class SecretResource {
    private final DockerClient docker;

    public SecretResource(DockerClient docker) {
        this.docker = docker;
    }

    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName,
                           String input) throws DockerException, InterruptedException, IOException {

        String secretsVolumeName = translateToVolumeName(imageName);
        String intermediaryContainerImage = "tianon/true";
        String intermediaryContainerName = "temp-container";
        String destinationPath = "/temp";

        ContainerCreation container = createIntermediaryContainer(intermediaryContainerName, intermediaryContainerImage, secretsVolumeName, destinationPath);

        String secretsTempDir = "secrets-temp-dir";
        String secretsFileName = "secrets";

        File tempDirectory = getTempSecretsDir(secretsTempDir);
        File tempFile = getTempSecretsFile(secretsFileName, tempDirectory);
        Files.write(new File(tempDirectory, tempFile.getName()).toPath(), input.getBytes());
        docker.copyToContainer(tempDirectory.toPath(), container.id(), destinationPath);

        tempFile.delete();
        tempDirectory.delete();
        docker.removeContainer(container.id());

        return Response.ok().build();
    }

    private String translateToVolumeName(String imageName) {
        int usernameSeparatorIndex = imageName.indexOf('/');
        int tagSeparatorIndex = imageName.lastIndexOf(':');
        StringBuilder sb = new StringBuilder(imageName);
        sb.setCharAt(usernameSeparatorIndex, '-');
        if (tagSeparatorIndex != -1) {
            sb.setCharAt(tagSeparatorIndex, '-');
        }

        return sb.toString();
    }

    private ContainerCreation createIntermediaryContainer(String intermediaryContainerName, String intermediaryContainerImage, String  secretsVolumeName, String destinationPath) throws DockerException, InterruptedException {
        HostConfig hostConfig = HostConfig.builder().binds(secretsVolumeName + ":" + destinationPath).build();
        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(intermediaryContainerImage)
                .build();
        return docker.createContainer(containerConfig, intermediaryContainerName);
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
