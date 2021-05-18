package conjob.resource.convert;

import conjob.api.JobRunConclusionResponse;
import conjob.api.JobRunResponse;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ResponseCreatorTest {
    private ResponseCreator responseCreator;

    @BeforeEach
    @BeforeTry
    void beforeEach() {
        responseCreator = new ResponseCreator();
    }

    @Property
    @Label("Given a job run response, " +
            "when creating a text web response from it, " +
            "should have the expected status, " +
            "and the body should be the given job run responses' output.")
    void createTextResponse(
            @ForAll("conclusionExpectedStatus") Map.Entry<JobRunConclusionResponse, Response.Status>
                    givenConclusionExpectedStatus,
            @ForAll String givenOutput,
            @ForAll Long givenExitCode,
            @ForAll String givenMessage) {
        JobRunConclusionResponse givenConclusion = givenConclusionExpectedStatus.getKey();
        Response.Status expectedResponseStatus = givenConclusionExpectedStatus.getValue();
        JobRunResponse jobRunResponse =
                new JobRunResponse(givenConclusion, givenOutput, givenExitCode, givenMessage);

        Response response = responseCreator.createResponseFrom(jobRunResponse);

        assertThat(response.getStatusInfo().toEnum(), is(expectedResponseStatus));
        assertThat(response.getEntity(), is(jobRunResponse.getOutput()));
    }

    @Property
    @Label("Given a job run response, " +
            "when creating a JSON web response from it, " +
            "should have the expected status, " +
            "and the body should be the given job run response.")
    void createJsonResponse(
            @ForAll("conclusionExpectedStatus") Map.Entry<JobRunConclusionResponse, Response.Status>
                    givenConclusionExpectedStatus,
            @ForAll String givenOutput,
            @ForAll Long givenExitCode,
            @ForAll String givenMessage) {
        JobRunConclusionResponse givenConclusion = givenConclusionExpectedStatus.getKey();
        Response.Status expectedResponseStatus = givenConclusionExpectedStatus.getValue();
        JobRunResponse jobRunResponse =
                new JobRunResponse(givenConclusion, givenOutput, givenExitCode, givenMessage);

        Response response = responseCreator.createJsonResponseFrom(jobRunResponse);

        assertThat(response.getStatusInfo().toEnum(), is(expectedResponseStatus));
        assertThat(response.getEntity(), is(jobRunResponse));
    }

    @Provide
    private Arbitrary<Map.Entry<JobRunConclusionResponse, Response.Status>> conclusionExpectedStatus() {
        return Arbitraries.oneOf(
                Arbitraries.of(Map.entry(JobRunConclusionResponse.SUCCESS, Response.ok().build().getStatusInfo().toEnum())),
                Arbitraries.of(Map.entry(JobRunConclusionResponse.FAILURE, Response.status(Response.Status.BAD_REQUEST).build().getStatusInfo().toEnum())),
                Arbitraries.of(Map.entry(JobRunConclusionResponse.NOT_FOUND, Response.status(Response.Status.NOT_FOUND).build().getStatusInfo().toEnum())),
                Arbitraries.of(Map.entry(JobRunConclusionResponse.REJECTED, Response.status(Response.Status.SERVICE_UNAVAILABLE).build().getStatusInfo().toEnum())),
                Arbitraries.of(Map.entry(JobRunConclusionResponse.TIMED_OUT, Response.status(Response.Status.REQUEST_TIMEOUT).build().getStatusInfo().toEnum())),
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.UNKNOWN,
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR).build().getStatusInfo().toEnum())));
    }
}