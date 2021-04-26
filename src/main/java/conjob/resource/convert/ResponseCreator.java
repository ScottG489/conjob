package conjob.resource.convert;

import conjob.api.JobRunConclusionResponse;
import conjob.api.JobRunResponse;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ResponseCreator {
    private final Map<JobRunConclusionResponse, Response.ResponseBuilder> jobRunConclusionResponseStatus;

    public ResponseCreator() {
        jobRunConclusionResponseStatus =
                Map.of(JobRunConclusionResponse.SUCCESS, Response.ok(),
                        JobRunConclusionResponse.FAILURE, Response.status(Response.Status.BAD_REQUEST),
                        JobRunConclusionResponse.NOT_FOUND, Response.status(Response.Status.NOT_FOUND),
                        JobRunConclusionResponse.REJECTED, Response.status(Response.Status.SERVICE_UNAVAILABLE),
                        JobRunConclusionResponse.TIMED_OUT, Response.status(Response.Status.REQUEST_TIMEOUT));
    }

    public Response createResponseFrom(JobRunResponse jobRunResponse) {
        return create(jobRunResponse.getConclusion())
                .entity(jobRunResponse.getOutput())
                .build();
    }

    public Response createJsonResponseFrom(JobRunResponse runResponse) {
        return create(runResponse.getConclusion())
                .entity(runResponse)
                .build();
    }

    private Response.ResponseBuilder create(JobRunConclusionResponse conclusionResponse) {
        return jobRunConclusionResponseStatus.getOrDefault(
                conclusionResponse,
                Response.status(Response.Status.INTERNAL_SERVER_ERROR));
    }
}
