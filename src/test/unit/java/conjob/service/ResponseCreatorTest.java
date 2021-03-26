package conjob.service;

import conjob.core.job.model.JobRunConclusion;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class ResponseCreatorTest {
    private ResponseCreator responseCreator;

    @BeforeEach
    @BeforeTry
    void setup() {
        responseCreator = new ResponseCreator();
    }

    @ParameterizedTest(name = "Given a JobRunConclusion, " +
            "when mapping it to a Response.Status, " +
            "should map {0}")
    @MethodSource("givenConclusionResponseStatus")
    void mapFromSuccess(Map.Entry<JobRunConclusion, Response.Status> givenConclusionResponseStatus) {
        JobRunConclusion givenConclusion = givenConclusionResponseStatus.getKey();
        Response.Status expectedResponseStatus = givenConclusionResponseStatus.getValue();
        Response.Status status = responseCreator.create(givenConclusion)
                .build().getStatusInfo().toEnum();

        assertThat(status, is(expectedResponseStatus));
    }

    @Property
    @Label("Given a JobRunConclusion, " +
            "when mapping it to a Response.Status, " +
            "should never be a server error")
    void shouldNeverMapToServerError(@ForAll JobRunConclusion jobRunConclusion) {
        Response.Status status = responseCreator.create(jobRunConclusion)
                .build().getStatusInfo().toEnum();

        assertThat(status, is(not(Response.Status.INTERNAL_SERVER_ERROR)));
    }

    private static List<Map.Entry<JobRunConclusion, Response.Status>> givenConclusionResponseStatus() {
        return Arrays.asList(
                Map.entry(JobRunConclusion.SUCCESS, Response.ok().build().getStatusInfo().toEnum()),
                Map.entry(JobRunConclusion.FAILURE, Response.status(Response.Status.BAD_REQUEST).build().getStatusInfo().toEnum()),
                Map.entry(JobRunConclusion.NOT_FOUND, Response.status(Response.Status.NOT_FOUND).build().getStatusInfo().toEnum()),
                Map.entry(JobRunConclusion.REJECTED, Response.status(Response.Status.SERVICE_UNAVAILABLE).build().getStatusInfo().toEnum()),
                Map.entry(JobRunConclusion.TIMED_OUT, Response.status(Response.Status.REQUEST_TIMEOUT).build().getStatusInfo().toEnum())
        );
    }
}