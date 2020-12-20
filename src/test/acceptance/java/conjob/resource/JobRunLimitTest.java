package conjob.resource;

import conjob.util.ConcurrentRequestUtil;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ResponseOptions;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static conjob.util.ConfigUtil.getFromConfig;
import static conjob.util.RestAssuredUtil.configTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JobRunLimitTest {
    private static final String JOB_RUN_PATH = "/job/run";
    private static final String CONFIG_TASK_PATH = "/tasks/config";

    Response originalConfigResponse;

    @Before
    public void setup() {
        configTest();
    }

    @After
    public void teardown() {
        updateServiceLimitConfig(originalConfigResponse.asString());
    }

    @Test
    public void testMaxRequestsPerSecond() throws InterruptedException, ExecutionException, TimeoutException {
        String imageName = "scottg489/echo-job:latest";

        int concurrentRequestCount = 6;

        int maxGlobalRequestsPerSecond = 3;
        int maxConcurrentRuns = 9999;
        int maxTimeoutSeconds = 9999;
        int maxKillTimeoutSeconds = 9999;

        originalConfigResponse = updateServiceLimitConfig(maxGlobalRequestsPerSecond, maxConcurrentRuns, maxTimeoutSeconds, maxKillTimeoutSeconds);

        RequestSpecification requestSpec = given()
                .accept(ContentType.JSON)
                .queryParam("image", imageName)
                .basePath(JOB_RUN_PATH);
        List<Response> responses = ConcurrentRequestUtil.runConcurrentRequests(requestSpec, concurrentRequestCount);
        List<Integer> statusCodes =
                responses.stream().map(ResponseOptions::getStatusCode).collect(Collectors.toList());

        assertThat(Collections.frequency(statusCodes, HttpStatus.SC_SERVICE_UNAVAILABLE), is(3));
        assertThat(findAllRejectedResponses(responses).count(), is(3L));
    }

    @Test
    public void testMaxConcurrentRequests() throws InterruptedException, ExecutionException, TimeoutException {
        String imageName = "scottg489/echo-job:latest";

        int concurrentRequestCount = 6;

        int maxGlobalRequestsPerSecond = 9999;
        int maxConcurrentRuns = 3;
        int maxTimeoutSeconds = 9999;
        int maxKillTimeoutSeconds = 9999;

        originalConfigResponse = updateServiceLimitConfig(maxGlobalRequestsPerSecond, maxConcurrentRuns, maxTimeoutSeconds, maxKillTimeoutSeconds);

        RequestSpecification requestSpec = given()
                .accept(ContentType.JSON)
                .queryParam("image", imageName)
                .basePath(JOB_RUN_PATH);
        List<Response> responses = ConcurrentRequestUtil.runConcurrentRequests(requestSpec, concurrentRequestCount);
        List<Integer> statusCodes =
                responses.stream().map(ResponseOptions::getStatusCode).collect(Collectors.toList());

        assertThat(Collections.frequency(statusCodes, HttpStatus.SC_SERVICE_UNAVAILABLE), is(3));
        assertThat(findAllRejectedResponses(responses).count(), is(3L));
    }

    @Test
    public void testMaxTimeoutExceededButFinishesBeforeKilled() {
        String imageName = "scottg489/sleep-job:latest";

        int jobLengthSeconds = 5;

        int maxGlobalRequestsPerSecond = 9999;
        int maxConcurrentRuns = 9999;
        int maxTimeoutSeconds = 3;
        int maxKillTimeoutSeconds = 10;

        originalConfigResponse = updateServiceLimitConfig(maxGlobalRequestsPerSecond, maxConcurrentRuns, maxTimeoutSeconds, maxKillTimeoutSeconds);

        given()
                .accept(ContentType.JSON)
                .body(jobLengthSeconds)
                .post(JOB_RUN_PATH + "?image=" + imageName)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("conclusion", is("SUCCESS"))
            .body("exitCode", is(0));
    }

    @Test
    public void testMaxTimeoutExceededAndKilledBeforeFinishing() {
        String imageName = "scottg489/sleep-job:latest";

        int jobLengthSeconds = 10;

        int maxGlobalRequestsPerSecond = 9999;
        int maxConcurrentRuns = 9999;
        int maxTimeoutSeconds = 3;
        int maxKillTimeoutSeconds = 2;

        originalConfigResponse = updateServiceLimitConfig(maxGlobalRequestsPerSecond, maxConcurrentRuns, maxTimeoutSeconds, maxKillTimeoutSeconds);

        given()
            .accept(ContentType.JSON)
            .body(jobLengthSeconds)
            .post(JOB_RUN_PATH + "?image=" + imageName)
        .then()
            .statusCode(HttpStatus.SC_REQUEST_TIMEOUT)
            .body("conclusion", is("TIMED_OUT"))
            .body("exitCode", is(-1));
    }

    private Stream<Response> findAllRejectedResponses(List<Response> responses) {
        return responses.stream().filter(response -> {
            return response.getBody().jsonPath().getString("conclusion").equals("REJECTED");
        });
    }

    private void updateServiceLimitConfig(String queryParams) {
        String adminBaseUri = getFromConfig("adminBaseUri");
        String adminUsername = getFromConfig("adminUsername");
        String adminPassword = getFromConfig("adminPassword");

        given()
            .baseUri(adminBaseUri)
            .auth().basic(adminUsername, adminPassword)
            .contentType(ContentType.URLENC)
            .body(queryParams)
            .post(CONFIG_TASK_PATH)
        .then()
            .statusCode(HttpStatus.SC_OK);
    }

    private Response updateServiceLimitConfig(
            int maxGlobalRequestsPerSecond,
            int maxConcurrentRuns,
            int maxTimeoutSeconds,
            int maxKillTimeoutSeconds) {
        String adminBaseUri = getFromConfig("adminBaseUri");
        String adminUsername = getFromConfig("adminUsername");
        String adminPassword = getFromConfig("adminPassword");

        Response post = given()
                .baseUri(adminBaseUri)
                .auth().basic(adminUsername, adminPassword)
                .queryParam("conjob.job.limit.maxGlobalRequestsPerSecond", maxGlobalRequestsPerSecond)
                .queryParam("conjob.job.limit.maxConcurrentRuns", maxConcurrentRuns)
                .queryParam("conjob.job.limit.maxTimeoutSeconds", maxTimeoutSeconds)
                .queryParam("conjob.job.limit.maxKillTimeoutSeconds", maxKillTimeoutSeconds)
                .post(CONFIG_TASK_PATH);
        post.then().statusCode(HttpStatus.SC_OK);
        return post;
    }
}
