package conjob.resource;

import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static conjob.util.RestAssuredUtil.configTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class JobTest {
    private static final String JOB_RUN_PATH = "/job/run";

    @Before
    public void setup() {
        configTest();
    }

    @Test
    public void getPlainTextResponse() {
        String expectContains = "Hello from Docker!";

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString(expectContains));

        given()
            .accept(ContentType.TEXT)
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString(expectContains));
    }

    @Test
    public void getJsonResponse() {
        String expectOutputContains = "Hello from Docker!";

        given()
            .accept(ContentType.JSON)
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON)
            .body("jobRun.output", containsString(expectOutputContains))
            .body("jobRun.exitCode", is(0))
            .body("result", is("FINISHED"));
    }

    @Test
    public void getNotFoundJsonResponse() {
        given()
            .accept(ContentType.JSON)
            .get(JOB_RUN_PATH + "?image=local/does_not_exist:latest")
       .then()
           .statusCode(404)
           .contentType(MediaType.APPLICATION_JSON)
           .body("jobRun.output", containsString(""))
           .body("jobRun.exitCode", is(-1))
           .body("result", is("NOT_FOUND"));
    }
}
