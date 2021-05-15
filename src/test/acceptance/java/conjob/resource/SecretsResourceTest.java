package conjob.resource;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static conjob.util.RestAssuredUtil.configTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SecretsResourceTest {
    private static final String SECRET_RUN_PATH = "/secret";
    private static final String JOB_RUN_PATH = "/job/run";
    private static final String TEST_SUPPORT_CONTAINER = "scottg489/test-support-job:latest";
    private static final String SECRET_FILE_MOUNT_LOCATION = "/run/build/secrets/secrets";

    @BeforeEach
    public void setup() {
        configTest();
    }

    @Test
    @DisplayName("Given a secret string, " +
            "and the job name it is for, " +
            "when creating the secret, " +
            "and running the given job, " +
            "should have access to the secret the job was associated with.")
    public void createSecret() {
        String givenSecret = "this_is_a_secret";

        given()
            .body(givenSecret)
            .post(SECRET_RUN_PATH + "?image=" + TEST_SUPPORT_CONTAINER)
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
