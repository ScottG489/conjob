package conjob.resource.secrets;

import conjob.ConJobApplication;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class SecretsResourceTest {
    private static final String SECRETS_PATH = "/secrets";
    private static final String JOB_RUN_PATH = "/job/run";
    private static final String TEST_SUPPORT_CONTAINER = "scottg489/test-support-job:latest";
    private static final String SECRET_FILE_MOUNT_LOCATION = "/run/build/secrets/secrets";

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
            "and a secret, " +
            "when when associating that secret with a job, " +
            "and running that job, " +
            "then the job should have access to the secret, " +
            "and it should be the same secret that was associated with it.")
    public void createSecrets() throws Exception {
        String givenSecret = "this_is_a_secret";

        app.run("server", "src/test/integration/resources/default_config.yml");

        given()
                .body(givenSecret)
                .post(SECRETS_PATH + "?image=" + TEST_SUPPORT_CONTAINER)
        .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .accept(ContentType.TEXT)
                .body("0||" + SECRET_FILE_MOUNT_LOCATION + "|0")
                .post(JOB_RUN_PATH + "?image=" + TEST_SUPPORT_CONTAINER)
        .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(is(givenSecret));
    }
}