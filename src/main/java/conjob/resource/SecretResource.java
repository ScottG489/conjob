package conjob.resource;

import com.spotify.docker.client.exceptions.DockerException;
import conjob.service.SecretsService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/secret")
@PermitAll
@Slf4j
public class SecretResource {
    private final SecretsService secretsService;

    public SecretResource(SecretsService secretsService) {
        this.secretsService = secretsService;
    }

    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName,
                           String input) throws DockerException, InterruptedException, IOException {
        secretsService.createSecret(imageName, input);
        return Response.ok().build();
    }
}
