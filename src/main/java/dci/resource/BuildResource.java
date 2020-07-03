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
    private final HostConfig hostConfig;
    private final DockerClient docker;

    public BuildResource(DockerClient docker, HostConfig hostConfig) {
        this.docker = docker;
        this.hostConfig = hostConfig;
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
}
