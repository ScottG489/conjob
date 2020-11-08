package dci.resource;

import com.spotify.docker.client.exceptions.DockerException;
import dci.core.job.JobService;
import dci.core.job.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/build")
@Slf4j
public class BuildResource {
    private final JobService jobService;

    public BuildResource(JobService jobService) {
        this.jobService = jobService;
    }

    @POST
    public Response doPost(@NotEmpty @QueryParam("image") String imageName,
                           @QueryParam("pull") @DefaultValue("true") boolean shouldPull,
                           String input) throws DockerException, InterruptedException {
        return getResponse(imageName, shouldPull, input);
    }

    @GET
    public Response doGet(@NotEmpty @QueryParam("image") String imageName,
                          @QueryParam("pull") @DefaultValue("true") boolean shouldPull)
            throws DockerException, InterruptedException {
        return getResponse(imageName, shouldPull, null);
    }

    private Response getResponse(@QueryParam("image") @NotEmpty String imageName,
                                 boolean shouldPull,
                                 String input) throws DockerException, InterruptedException {
        log.info("Running image: '{}'", imageName);
        Job job = jobService.getJob(imageName, input, shouldPull);

        return getResponseBuilderWithExitStatus(job.getJobRun().getExitCode())
                .entity(job.getJobRun().getOutput())
                .build();
    }

    private Response.ResponseBuilder getResponseBuilderWithExitStatus(long exitCode) throws DockerException, InterruptedException {
        Response.ResponseBuilder responseBuilder;

        if (exitCode == 0) {
            responseBuilder = Response.ok();
        } else {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        }

        return responseBuilder;
    }
}
