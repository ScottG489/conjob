package dci.resource;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/build")
@Slf4j
public class BuildResource {
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient docker;

    public BuildResource(DockerClient docker) {
        this.docker = docker;
    }

    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName,
                           @QueryParam("pull") @DefaultValue("true") boolean shouldPull,
                           String input) throws DockerException, InterruptedException {
        return getResponse(imageName, shouldPull, input);
    }

    @GET
    public Response doGet(@NotEmpty @QueryParam("image") String imageName,
                          @QueryParam("pull") @DefaultValue("true") boolean shouldPull) throws DockerException, InterruptedException {
        return getResponse(imageName, shouldPull, null);
    }

    private Response getResponse(@QueryParam("image") @NotEmpty String imageName,
                                 boolean shouldPull,
                                 String input) throws DockerException, InterruptedException {
        log.info("Running image: '{}'", imageName);
        if (shouldPull) {
            docker.pull(imageName);
        }

        String secretsVolumeName = translateToVolumeName(imageName);
        HostConfig hostConfig = getHostConfig(secretsVolumeName);
        ContainerConfig.Builder builder = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig);
        if (input != null && !input.isEmpty()) {
            builder.cmd(input);
        }
        final ContainerConfig containerConfig = builder.build();
        final ContainerCreation container = docker.createContainer(containerConfig);

        docker.startContainer(container.id());
        // TODO: This seems to prevent the output from being a streaming response
//        docker.stopContainer(container.id(), 600);  // 10 minutes

        LogStream logs = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr(), DockerClient.LogsParam.follow());
        String output = logs.readFully();

        return getResponseBuilderWithExitStatus(container)
                .entity(output)
                .build();
    }

    private Response.ResponseBuilder getResponseBuilderWithExitStatus(ContainerCreation container) throws DockerException, InterruptedException {
        Response.ResponseBuilder responseBuilder;

        Long exitCode = docker.inspectContainer(container.id()).state().exitCode();

        if (exitCode == 0) {
            responseBuilder = Response.ok();
        } else {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        }

        return responseBuilder;
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

    private HostConfig getHostConfig(String secretsVolumeName) throws DockerException, InterruptedException {
        HostConfig.Builder builder = HostConfig.builder()
                .appendBinds("/var/run/docker.sock:/var/run/docker.sock");

        docker.listVolumes().volumes().stream()
                .filter(volume -> volume.name().equals(secretsVolumeName))
                .limit(1)
                .forEach(volume -> {
                    builder.appendBinds(secretsVolumeName + ":" + SECRETS_VOLUME_MOUNT_PATH + ":" + SECRETS_VOLUME_MOUNT_OPTIONS);
                });

        return builder.build();
    }
}
