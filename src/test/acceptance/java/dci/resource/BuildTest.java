package dci.resource;

import dci.api.JobResponse;
import dci.api.JobResultResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static dci.util.RestAssuredUtil.configTest;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BuildTest {
    @Before
    public void setup() {
        configTest();
    }

    @Test
    public void getPlainTextResponse() {
        String expectContains = "Hello from Docker!";

        ExtractableResponse<Response> response = RestAssured
                .get("/build?image=library/hello-world:latest")
                .then()
                    .extract();

        assertThat(response.contentType(), is(MediaType.TEXT_PLAIN));
        assertThat(response.body().asString(), containsString(expectContains));
    }

    @Test
    public void getJsonResponse() {
        String expectOutputContains = "Hello from Docker!";

        ExtractableResponse<Response> response = RestAssured
                .given()
                    .accept(ContentType.JSON)
                .get("/build?image=library/hello-world:latest")
                .then()
                    .extract();

        assertThat(response.contentType(), is(MediaType.APPLICATION_JSON));

        JobResponse jobResponse = response.as(JobResponse.class);

        assertThat(jobResponse.getJobRun().getOutput(), containsString(expectOutputContains));
        assertThat(jobResponse.getJobRun().getExitCode(), is(0L));

        assertThat(jobResponse.getResult(), is(JobResultResponse.FINISHED));
    }
}
