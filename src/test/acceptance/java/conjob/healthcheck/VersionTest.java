package conjob.healthcheck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static conjob.util.RestAssuredUtil.configAdminTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesRegex;

public class VersionTest {
    @BeforeEach
    public void setup() {
        configAdminTest();
    }

    @Test
    public void testVersion() {
        given()
                .get("/healthcheck")
        .then()
                .body("version.message", matchesRegex("[0-9a-f]{5,40}"));
    }
}
