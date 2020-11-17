package conjob.resource;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import conjob.core.job.config.ConfigUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Path("/secret")
@PermitAll
@Slf4j
public class SecretResource {
    private static final String INTERMEDIARY_CONTAINER_IMAGE = "tianon/true";
    private final DockerClient docker;

    public SecretResource(DockerClient docker) throws DockerException, InterruptedException {
        this.docker = docker;
        // This image is required to be on the build server to create secrets
        docker.pull(INTERMEDIARY_CONTAINER_IMAGE);
    }

    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName,
                           String input) throws DockerException, InterruptedException, IOException {

        String secretsVolumeName = new ConfigUtil().translateToVolumeName(imageName);
        String intermediaryContainerName = "temp-container";
        String destinationPath = "/temp";

        ContainerCreation container = createIntermediaryContainer(
                intermediaryContainerName,
                INTERMEDIARY_CONTAINER_IMAGE,
                secretsVolumeName,
                destinationPath);

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

    private ContainerCreation createIntermediaryContainer(String intermediaryContainerName, String intermediaryContainerImage, String secretsVolumeName, String destinationPath) throws DockerException, InterruptedException {
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
