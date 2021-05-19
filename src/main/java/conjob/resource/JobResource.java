package conjob.resource;

import conjob.core.job.model.JobRun;
import conjob.core.secrets.SecretsStoreException;
import conjob.resource.convert.JobResponseConverter;
import conjob.resource.convert.ResponseCreator;
import conjob.service.job.JobService;
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
    private final ResponseCreator responseCreator;
    private final JobResponseConverter jobResponseConverter;

    public JobResource(
            JobService jobService,
            ResponseCreator responseCreator,
            JobResponseConverter jobResponseConverter) {
        this.jobService = jobService;
        this.responseCreator = responseCreator;
        this.jobResponseConverter = jobResponseConverter;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response handleTextPost(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            String input,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws SecretsStoreException {
        return createResponse(imageName, input, pullStrategy);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.WILDCARD + ";q=0"})
    public Response handleJsonPost(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            String input,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws SecretsStoreException {
        return createJsonResponse(imageName, input, pullStrategy);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response handleTextGet(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws SecretsStoreException {
        return createResponse(imageName, "", pullStrategy);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.WILDCARD + ";q=0"})
    public Response handleJsonGet(
            @NotEmpty @Pattern(regexp = DOCKER_IMAGE_NAME_FORMAT) @QueryParam("image") String imageName,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy)
            throws SecretsStoreException {
        return createJsonResponse(imageName, "", pullStrategy);
    }

    private Response createResponse(String imageName, String input, String pullStrategy)
            throws SecretsStoreException {
        log.info("Running image: '{}'", imageName);
        JobRun jobRun = jobService.runJob(imageName, input, pullStrategy);
        log.info("Job run finished: '{}'", jobRun);
        return responseCreator.createResponseFrom(jobResponseConverter.from(jobRun));
    }

    private Response createJsonResponse(String imageName, String input, String pullStrategy)
            throws SecretsStoreException {
        log.info("Running image: '{}'", imageName);
        JobRun jobRun = jobService.runJob(imageName, input, pullStrategy);
        return responseCreator.createJsonResponseFrom(jobResponseConverter.from(jobRun));
    }
}