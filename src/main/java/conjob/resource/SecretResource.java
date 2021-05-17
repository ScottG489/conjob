package conjob.resource;

import com.spotify.docker.client.exceptions.DockerException;
import conjob.service.SecretsService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/secret")
@PermitAll
@Slf4j
public class SecretResource {
    private static final String DOCKER_IMAGE_NAME_FORMAT = "^(?:(?=[^:\\/]{1,253})(?!-)[a-zA-Z0-9-]{1,63}(?<!-)(?:\\.(?!-)[a-zA-Z0-9-]{1,63}(?<!-))*(?::[0-9]{1,5})?/)?((?![._-])(?:[a-z0-9._-]*)(?<![._-])(?:/(?![._-])[a-z0-9._-]*(?<![._-]))*)(?::(?![.-])[a-zA-Z0-9_.-]{1,128})$";

    private final SecretsService secretsService;

    public SecretResource(SecretsService secretsService) {
        this.secretsService = secretsService;
    }

    @POST

    public Response doPost(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            String input) throws IOException {
        secretsService.createSecret(imageName, input);
        return Response.ok().build();
    }
}
