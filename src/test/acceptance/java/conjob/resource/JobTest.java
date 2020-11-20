package conjob.resource;

import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static conjob.util.RestAssuredUtil.configTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class JobTest {
    private static final String JOB_RUN_PATH = "/job/run";

    @Before
    public void setup() {
        configTest();
    }

    @Test
    public void getPlainTextResponse() {
        String expectStartsWith = "\nHello from Docker!";

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));

        given()
            .accept(ContentType.TEXT)
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(containsString(expectStartsWith));
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

    @Test
    public void getPullResponses() {
        String expectStartsWith = "\nHello from Docker!";

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&pull=always")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&pull=never")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&pull=absent")
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));
    }

    @Test
    public void getPostResponses() {
        String echoImage = "scottg489/echo-job:latest";
        String expectedResponse = "foobar";

        given()
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(is(""));

        given()
            .body("foobar")
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(200)
            .contentType(MediaType.TEXT_PLAIN)
            .body(is("foobar"));
    }

    @Test
    public void getJsonPostResponses() {
        String echoImage = "scottg489/echo-job:latest";

        given()
            .accept(ContentType.JSON)
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON)
            .body("jobRun.output", is(""))
            .body("jobRun.exitCode", is(0))
            .body("result", is("FINISHED"))
            .body("message", is("Job has concluded. Check job run for outcome."));

        given()
            .accept(ContentType.JSON)
            .body("foobar")
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON)
            .body("jobRun.output", is("foobar"))
            .body("jobRun.exitCode", is(0))
            .body("result", is("FINISHED"))
            .body("message", is("Job has concluded. Check job run for outcome."));
    }
}
