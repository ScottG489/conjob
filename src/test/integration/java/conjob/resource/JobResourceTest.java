package conjob.resource;

import conjob.ConJobApplication;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.startsWith;

public class JobResourceTest {
    private static final String JOB_RUN_PATH = "/job/run";

    private ConJobApplication app;

    @BeforeEach
    public void beforeEach() {
        app = new ConJobApplication();
    }

    @AfterEach
    public void afterEach() throws Exception {
        app.getEnvironment().getApplicationContext().getServer().stop();
    }

    @Test
    public void getPlainTextResponseWithoutTag() throws Exception {
        String expectStartsWith = "\nHello from Docker!";

        app.run("server", "src/test/integration/resources/default_config.yml");

        given()
                .get(JOB_RUN_PATH + "?image=library/hello-world")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(startsWith(expectStartsWith));
    }
}
