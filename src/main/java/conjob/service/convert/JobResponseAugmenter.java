package conjob.service.convert;

import conjob.core.job.model.Job;
import conjob.core.job.model.JobResult;

import javax.ws.rs.core.Response;

public class JobResponseAugmenter {
    public Response.ResponseBuilder create(Job job) {
        Response.ResponseBuilder responseBuilder;
        JobResult jobResult = job.getResult();
        long exitCode = job.getJobRun().getExitCode();

        if (jobResult.equals(JobResult.FINISHED)) {
            if (exitCode == 0) {
                responseBuilder = Response.ok();
            } else {
                responseBuilder = Response.status(Response.Status.BAD_REQUEST);
            }
        } else if (jobResult.equals(JobResult.NOT_FOUND)) {
            responseBuilder = Response.status(Response.Status.NOT_FOUND);
        } else if (jobResult.equals(JobResult.KILLED)) {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        } else if (jobResult.equals(JobResult.REJECTED)) {
            responseBuilder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
        } else {
            responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return responseBuilder;
    }
}
