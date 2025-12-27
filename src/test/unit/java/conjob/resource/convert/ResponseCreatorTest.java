package conjob.resource.convert;

import conjob.api.JobRunConclusionResponse;
import conjob.api.JobRunResponse;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;

import jakarta.ws.rs.core.Response;
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

        assertThat(response.getStatusInfo(), is(expectedResponseStatus));
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

        assertThat(response.getStatusInfo(), is(expectedResponseStatus));
        assertThat(response.getEntity(), is(jobRunResponse));
    }

    @Provide
    private Arbitrary<Map.Entry<JobRunConclusionResponse, Response.Status>> conclusionExpectedStatus() {
        return Arbitraries.oneOf(
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.SUCCESS,
                        Response.Status.OK)),
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.FAILURE,
                        Response.Status.BAD_REQUEST)),
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.NOT_FOUND,
                        Response.Status.NOT_FOUND)),
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.REJECTED,
                        Response.Status.SERVICE_UNAVAILABLE)),
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.TIMED_OUT,
                        Response.Status.REQUEST_TIMEOUT)),
                Arbitraries.of(Map.entry(
                        JobRunConclusionResponse.UNKNOWN,
                        Response.Status.INTERNAL_SERVER_ERROR)));
    }
}