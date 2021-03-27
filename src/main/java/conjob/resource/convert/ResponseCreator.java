package conjob.resource.convert;

import conjob.core.job.model.JobRunConclusion;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ResponseCreator {
    private final Map<JobRunConclusion, Response.ResponseBuilder> jobRunConclusionResponseStatus;

    public ResponseCreator() {
        jobRunConclusionResponseStatus =
                Map.of(JobRunConclusion.SUCCESS, Response.ok(),
                        JobRunConclusion.FAILURE, Response.status(Response.Status.BAD_REQUEST),
                        JobRunConclusion.NOT_FOUND, Response.status(Response.Status.NOT_FOUND),
                        JobRunConclusion.REJECTED, Response.status(Response.Status.SERVICE_UNAVAILABLE),
                        JobRunConclusion.TIMED_OUT, Response.status(Response.Status.REQUEST_TIMEOUT)
                );
    }

    public Response.ResponseBuilder create(JobRunConclusion conclusion) {
        return jobRunConclusionResponseStatus.getOrDefault(
                conclusion,
                Response.status(Response.Status.INTERNAL_SERVER_ERROR));
    }
}
