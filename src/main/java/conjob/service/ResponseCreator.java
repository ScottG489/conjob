package conjob.service;

import conjob.core.job.model.JobRunConclusion;

import javax.ws.rs.core.Response;

public class ResponseCreator {
    public Response.ResponseBuilder create(JobRunConclusion conclusion) {
        Response.ResponseBuilder responseBuilder;

        if (conclusion.equals(JobRunConclusion.SUCCESS)) {
            responseBuilder = Response.ok();
        } else if (conclusion.equals(JobRunConclusion.FAILURE) || conclusion.equals(JobRunConclusion.REJECTED)) {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        } else if (conclusion.equals(JobRunConclusion.NOT_FOUND)) {
            responseBuilder = Response.status(Response.Status.NOT_FOUND);
        } else if (conclusion.equals(JobRunConclusion.TIMED_OUT)) {
            responseBuilder = Response.status(Response.Status.REQUEST_TIMEOUT);
        } else {
            responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return responseBuilder;
    }
}
