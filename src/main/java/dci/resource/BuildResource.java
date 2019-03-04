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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/build")
//@Slf4j
public class BuildResource {
    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName, String input) throws DockerCertificateException, DockerException, InterruptedException {
        return getResponse(imageName, input);
    }

    @GET
    public Response doGet(@NotEmpty @QueryParam("image") String imageName) throws DockerCertificateException, DockerException, InterruptedException {
        return getResponse(imageName, null);
    }

    private Response getResponse(@QueryParam("image") @NotEmpty String imageName, String input) throws DockerCertificateException, DockerException, InterruptedException {
        //        log.invokeMethod("info", new Object[]{"Info resource triggered."});

        final DockerClient docker = DefaultDockerClient.fromEnv().build();

        docker.pull(imageName);

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

        LogStream logs = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr(), DockerClient.LogsParam.follow());

        return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .entity(logs.readFully())
                .build();
    }
}
