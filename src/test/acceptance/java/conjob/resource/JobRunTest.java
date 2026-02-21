package conjob.resource;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import static conjob.util.RestAssuredUtil.configTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class JobRunTest {
    private static final String JOB_RUN_PATH = "/job/run";

    @BeforeEach
    public void setup() {
        configTest();
    }

    @Test
    public void getPlainTextResponse() {
        String expectStartsWith = "\nHello from Docker!";

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));

        given()
            .accept(ContentType.TEXT)
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
            .statusCode(HttpStatus.SC_OK)
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
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body("output", containsString(expectOutputContains))
            .body("exitCode", is(0))
            .body("conclusion", is("SUCCESS"));
    }

    @Test
    public void getNotFoundJsonResponse() {
        given()
            .accept(ContentType.JSON)
            .get(JOB_RUN_PATH + "?image=local/does_not_exist:latest")
        .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body("output", containsString(""))
            .body("exitCode", is(-1))
            .body("conclusion", is("NOT_FOUND"));
    }

    @Test
    public void getPullResponses() {
        String expectStartsWith = "\nHello from Docker!";

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&pull=always")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&pull=never")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&pull=absent")
        .then()
            .statusCode(HttpStatus.SC_OK)
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
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.TEXT_PLAIN)
            .body(is(""));

        given()
            .body("foobar")
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.TEXT_PLAIN)
            .body(is("foobar"));
    }

    @Test
    public void getRemoveContainerResponse() {
        String expectStartsWith = "\nHello from Docker!";

        given()
            .get(JOB_RUN_PATH + "?image=library/hello-world:latest&remove=true")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.TEXT_PLAIN)
            .body(startsWith(expectStartsWith));
    }

    @Test
    public void getJsonPostResponses() {
        String echoImage = "scottg489/echo-job:latest";

        given()
            .accept(ContentType.JSON)
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body("output", is(""))
            .body("exitCode", is(0))
            .body("conclusion", is("SUCCESS"))
            .body("message", is("Job run successful."));

        given()
            .accept(ContentType.JSON)
            .body("foobar")
            .post(JOB_RUN_PATH + "?image=" + echoImage)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body("output", is("foobar"))
            .body("exitCode", is(0))
            .body("conclusion", is("SUCCESS"))
            .body("message", is("Job run successful."));
    }
}
