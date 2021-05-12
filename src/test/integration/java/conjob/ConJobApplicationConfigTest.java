package conjob;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class ConJobApplicationConfigTest {
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
    @DisplayName("Given the default config, " +
            "when running the application, " +
            "then it should start successfully, " +
            "and successfully process a simple request.")
    public void defaultConfig() throws Exception {
        String expectStartsWith = "\nHello from Docker!";

        app.run("server", "src/test/integration/resources/default_config.yml");

        given()
                .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(startsWith(expectStartsWith));
    }

    @Test
    @DisplayName("Given a config with basic auth, " +
            "when running the application, " +
            "then it should start successfully, " +
            "and successfully process a request with basic auth, " +
            "and fail to process a request with no auth.")
    public void basicAuthConfig() throws Exception {
        String expectStartsWith = "\nHello from Docker!";

        app.run("server", "src/test/integration/resources/basic_auth_config.yml");

        given()
                .auth().basic("basic_username", "basic_password")
                .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(startsWith(expectStartsWith));

        given()
                .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Given a config with admin basic auth, " +
            "when running the application, " +
            "then it should start successfully, " +
            "and successfully process an admin request with basic auth," +
            "and fail to do the same with no auth.")
    public void adminBasicAuthConfig() throws Exception {
        String expectStartsWith = "pong";

        app.run("server", "src/test/integration/resources/admin_basic_auth_config.yml");

        given()
                .port(8081)
                .auth().basic("admin_basic_username", "admin_basic_password")
                .get("ping")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(is("pong\n"));

        given()
                .port(8081)
                .get("ping")
        .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}