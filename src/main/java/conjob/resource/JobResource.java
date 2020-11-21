package conjob.resource;

import com.spotify.docker.client.exceptions.DockerException;
import conjob.service.JobService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/job/run")
@PermitAll
@Slf4j
public class JobResource {
    // Taken from here: https://stackoverflow.com/a/39672069/14146969   Second to last char (?) removed to require
    //   user to specify tag. See here for why: https://github.com/ScottG489/conjob/issues/18
    private static final String DOCKER_IMAGE_NAME_FORMAT = "^(?:(?=[^:\\/]{1,253})(?!-)[a-zA-Z0-9-]{1,63}(?<!-)(?:\\.(?!-)[a-zA-Z0-9-]{1,63}(?<!-))*(?::[0-9]{1,5})?/)?((?![._-])(?:[a-z0-9._-]*)(?<![._-])(?:/(?![._-])[a-z0-9._-]*(?<![._-]))*)(?::(?![.-])[a-zA-Z0-9_.-]{1,128})$";
    private final JobService jobService;

    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @POST
    @Produces({MediaType.WILDCARD, MediaType.TEXT_PLAIN})
    public Response handlePost(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            String input,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws DockerException, InterruptedException {
        return createResponse(imageName, input, pullStrategy);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleJsonPost(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            String input,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws DockerException, InterruptedException {
        return createJsonResponse(imageName, input, pullStrategy);
    }

    @GET
    @Produces({MediaType.WILDCARD, MediaType.TEXT_PLAIN})
    public Response handleGet(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws DockerException, InterruptedException {
        return createResponse(imageName, "", pullStrategy);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleJsonGet(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws DockerException, InterruptedException {
        return createJsonResponse(imageName, "", pullStrategy);
    }

    private Response createResponse(String imageName, String input, String pullStrategy)
            throws DockerException, InterruptedException {
        log.info("Running image: '{}'", imageName);
        return jobService.createResponse(imageName, input, pullStrategy);
    }

    private Response createJsonResponse(String imageName, String input, String pullStrategy)
            throws DockerException, InterruptedException {
        log.info("Running image: '{}'", imageName);
        return jobService.createJsonResponse(imageName, input, pullStrategy);
    }

}
