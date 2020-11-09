package dci.resource;

import com.spotify.docker.client.exceptions.DockerException;
import dci.api.JobResponse;
import dci.core.job.JobService;
import dci.core.job.model.Job;
import dci.resource.convert.JobResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/build")
@Slf4j
public class BuildResource {
    private final JobService jobService;

    public BuildResource(JobService jobService) {
        this.jobService = jobService;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response handlePost(@NotEmpty @QueryParam("image") String imageName,
                               @QueryParam("pull") @DefaultValue("true") boolean shouldPull,
                               String input) throws DockerException, InterruptedException {
        return createResponse(imageName, input);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleJsonPost(@NotEmpty @QueryParam("image") String imageName,
                                   @QueryParam("pull") @DefaultValue("true") boolean shouldPull,
                                   String input) throws DockerException, InterruptedException {
        return createJsonResponse(imageName, shouldPull, input);
    }

    @GET
    @Produces({MediaType.WILDCARD, MediaType.TEXT_PLAIN})
    public Response handleGet(@NotEmpty @QueryParam("image") String imageName,
                              @QueryParam("pull") @DefaultValue("true") boolean shouldPull)
            throws DockerException, InterruptedException {
        return createResponse(imageName, null);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleJsonGet(@NotEmpty @QueryParam("image") String imageName,
                                  @QueryParam("pull") @DefaultValue("true") boolean shouldPull)
            throws DockerException, InterruptedException {
        return createJsonResponse(imageName, shouldPull, null);
    }

    private Response createResponse(String imageName,
                                    String input) throws DockerException, InterruptedException {
        log.info("Running image: '{}'", imageName);
        Job job = jobService.getJob(imageName, input);

        return getResponseBuilderWithExitStatus(job.getJobRun().getExitCode())
                .entity(job.getJobRun().getOutput())
                .build();
    }

    private Response createJsonResponse(String imageName,
                                        boolean shouldPull,
                                        String input) throws DockerException, InterruptedException {
        log.info("Running image: '{}'", imageName);
        Job job = jobService.getJob(imageName, input);

        JobResponse jobResponse = new JobResponseConverter().from(job);

        return getResponseBuilderWithExitStatus(job.getJobRun().getExitCode())
                .entity(jobResponse)
                .build();
    }


    private Response.ResponseBuilder getResponseBuilderWithExitStatus(long exitCode) {
        Response.ResponseBuilder responseBuilder;

        if (exitCode == 0) {
            responseBuilder = Response.ok();
        } else {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        }

        return responseBuilder;
    }
}
