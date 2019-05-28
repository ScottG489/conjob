package dci.resource;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

@Path("/build")
//@Slf4j
public class BuildResource {
    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName,
                           @QueryParam("pull") @DefaultValue("true") boolean shouldPull,
                           String input) throws DockerCertificateException, DockerException, InterruptedException {
        return getResponse(imageName, shouldPull, input);
    }

    @GET
    public Response doGet(@NotEmpty @QueryParam("image") String imageName,
                          @QueryParam("pull") @DefaultValue("true") boolean shouldPull) throws DockerCertificateException, DockerException, InterruptedException {
        return getResponse(imageName, shouldPull, null);
    }

    private Response getResponse(@QueryParam("image") @NotEmpty String imageName,
                                 boolean shouldPull,
                                 String input) throws DockerCertificateException, DockerException, InterruptedException {
        //        log.invokeMethod("info", new Object[]{"Info resource triggered."});

        final DockerClient docker = DefaultDockerClient.fromEnv().build();

        if (shouldPull) {
            docker.pull(imageName);
        }

        HostConfig hostConfig = HostConfig.builder().binds("/var/run/docker.sock:/var/run/docker.sock").build();
        ContainerConfig.Builder builder = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig);
        if (input != null && !input.isEmpty()) {
            builder.cmd(input);
        }
        final ContainerConfig containerConfig = builder.build();
        final ContainerCreation container = docker.createContainer(containerConfig);

        docker.startContainer(container.id());
        docker.stopContainer(container.id(), 600);  // 10 minutes

        LogStream logs = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr(), DockerClient.LogsParam.follow());
        StreamingOutput stream = os -> logs.attach(os, os);

        return Response.ok(stream)
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }
}
