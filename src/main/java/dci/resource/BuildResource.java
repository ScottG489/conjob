package dci.resource;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.TEXT_PLAIN)
//@Slf4j
public class BuildResource {
    @GET
    public Response doAction(@NotEmpty @QueryParam("image") String imageName, @QueryParam("secretKey") String secretKey) throws DockerCertificateException, DockerException, InterruptedException {
//        log.invokeMethod("info", new Object[]{"Info resource triggered."});

        final DockerClient docker = DefaultDockerClient.fromEnv().build();

        docker.pull(imageName);

        final ContainerConfig containerConfig = ContainerConfig.builder()
                .image(imageName)
                .cmd(secretKey)
                .build();
        final ContainerCreation container = docker.createContainer(containerConfig);

        docker.startContainer(container.id());
        
        LogStream logs = docker.logs(container.id(), DockerClient.LogsParam.stdout());

        return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .entity(logs.readFully())
                .build();
    }
}
