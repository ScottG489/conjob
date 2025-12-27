package conjob.resource;

import conjob.service.secrets.SecretsService;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
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
        log.info("Creating secrets for image: '{}'", imageName);
        secretsService.createsSecret(imageName, input);
        return Response.ok().build();
    }
}
