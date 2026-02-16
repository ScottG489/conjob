package conjob.resource;

import conjob.core.job.model.JobRun;
import conjob.core.secrets.SecretsStoreException;
import conjob.resource.convert.JobResponseConverter;
import conjob.resource.convert.ResponseCreator;
import conjob.service.job.JobService;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/job/run")
@PermitAll
@Slf4j
public class JobResource {
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
            @NotEmpty @QueryParam("image") String imageName,
            String input,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy,
            @QueryParam("use_docker_cache") @DefaultValue("true") boolean useDockerCache)
            throws SecretsStoreException {
        return createResponse(imageName, input, pullStrategy, useDockerCache);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.WILDCARD + ";q=0"})
    public Response handleJsonPost(
            @NotEmpty @QueryParam("image") String imageName,
            String input,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy,
            @QueryParam("use_docker_cache") @DefaultValue("true") boolean useDockerCache)
            throws SecretsStoreException {
        return createJsonResponse(imageName, input, pullStrategy, useDockerCache);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response handleTextGet(
            @NotEmpty @QueryParam("image") String imageName,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy,
            @QueryParam("use_docker_cache") @DefaultValue("true") boolean useDockerCache)
            throws SecretsStoreException {
        return createResponse(imageName, "", pullStrategy, useDockerCache);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.WILDCARD + ";q=0"})
    public Response handleJsonGet(
            @NotEmpty @QueryParam("image") String imageName,
            @QueryParam("pull") @DefaultValue("always") String pullStrategy,
            @QueryParam("use_docker_cache") @DefaultValue("true") boolean useDockerCache)
            throws SecretsStoreException {
        return createJsonResponse(imageName, "", pullStrategy, useDockerCache);
    }

    private Response createResponse(String imageName, String input, String pullStrategy, boolean useDockerCache)
            throws SecretsStoreException {
        log.info("Running image: '{}'", imageName);
        JobRun jobRun = jobService.runJob(imageName, input, pullStrategy, useDockerCache);
        log.info("Job run finished: '{}'", jobRun);
        return responseCreator.createResponseFrom(jobResponseConverter.from(jobRun));
    }

    private Response createJsonResponse(String imageName, String input, String pullStrategy, boolean useDockerCache)
            throws SecretsStoreException {
        log.info("Running image: '{}'", imageName);
        JobRun jobRun = jobService.runJob(imageName, input, pullStrategy, useDockerCache);
        return responseCreator.createJsonResponseFrom(jobResponseConverter.from(jobRun));
    }
}
