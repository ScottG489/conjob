package dci.resource;

import dci.api.JobResponse;
import dci.api.JobResultResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;

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

        String responseBody =
                RestAssured.get("/build?image=library/hello-world:latest").body().asString();

        assertThat(responseBody, containsString(expectContains));
    }

    @Test
    public void getJsonResponse() {
        String expectOutputContains = "Hello from Docker!";

        JobResponse jobResponse = RestAssured
                .given()
                    .accept(ContentType.JSON)
                .get("/build?image=library/hello-world:latest")
                .then()
                    .extract()
                    .as(JobResponse.class);


        assertThat(jobResponse.getJobRun().getOutput(), containsString(expectOutputContains));
        assertThat(jobResponse.getJobRun().getExitCode(), is(0L));

        assertThat(jobResponse.getResult(), is(JobResultResponse.FINISHED));
    }
}
