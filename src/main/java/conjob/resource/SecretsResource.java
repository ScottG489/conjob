package conjob.resource;

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

@Path("/secrets")
@PermitAll
@Slf4j
public class SecretsResource {
    private static final String DOCKER_IMAGE_NAME_FORMAT = "^(?:(?=[^:\\/]{1,253})(?!-)[a-zA-Z0-9-]{1,63}(?<!-)(?:\\.(?!-)[a-zA-Z0-9-]{1,63}(?<!-))*(?::[0-9]{1,5})?/)?((?![._-])(?:[a-z0-9._-]*)(?<![._-])(?:/(?![._-])[a-z0-9._-]*(?<![._-]))*)(?::(?![.-])[a-zA-Z0-9_.-]{1,128})$";

    private final SecretsService secretsService;

    public SecretsResource(SecretsService secretsService) {
        this.secretsService = secretsService;
    }

    @POST

    public Response handlePost(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            String input) throws IOException {
        secretsService.createsSecret(imageName, input);
        return Response.ok().build();
    }
}
