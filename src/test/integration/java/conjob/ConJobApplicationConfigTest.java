package conjob;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import conjob.resource.JobResource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConJobApplicationConfigTest {
    private static final String JOB_RUN_PATH = "/job/run";
    private static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";

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

        List<ILoggingEvent> logEvents = initLogAppenders();

        given()
                .get(JOB_RUN_PATH + "?image=library/hello-world:latest")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .header(TRACE_ID_HEADER_NAME, matchesPattern("[0-9a-f]{12}"))
                .body(startsWith(expectStartsWith));

        assertThat(hasTraceId(logEvents), is(true));
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

    private List<ILoggingEvent> initLogAppenders() {
        Logger logger = (Logger) LoggerFactory.getLogger(JobResource.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        return listAppender.list;
    }

    private boolean hasTraceId(List<ILoggingEvent> logEvents) {
        return logEvents.stream().map(ILoggingEvent::getMDCPropertyMap)
                .anyMatch(e -> e.get("traceId") != null);
    }
}