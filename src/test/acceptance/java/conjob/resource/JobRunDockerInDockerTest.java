package conjob.resource;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

import static conjob.util.ConfigUtil.getFromConfig;
import static conjob.util.RestAssuredUtil.configTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class JobRunDockerInDockerTest {
    private static final String JOB_RUN_PATH = "/job/run";
    private static final String DOCKER_VOLUME_RM_TASK_PATH = "/tasks/docker/volume/rm";

    @BeforeEach
    public void setup() {
        configTest();
    }

    @AfterEach
    public void teardown() {
        cleanUpVolumes("conjob-docker-cache-scottg489-docker-test-support-job-latest");
    }

    @Test
    @DisplayName("Given an image that builds and runs a container, " +
            "when it's run twice, " +
            "then the docker cache from the first run should be available to the second run."
    )
    public void buildAndRunNestedDockerContainer() {
        Response response = given()
                .get(JOB_RUN_PATH + "?image=scottg489/docker-test-support-job:latest");
        response.then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN);

        List<String> responseLines = response.asString().lines().collect(Collectors.toList());
        String originalImageId = responseLines.get(0);
        String originalContainerId = responseLines.get(1);

        Response response2 = given()
                .get(JOB_RUN_PATH + "?image=scottg489/docker-test-support-job:latest");
        response2.then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN);

        List<String> response2Lines = response2.asString().lines().collect(Collectors.toList());
        String existingImageId = response2Lines.get(0);
        String existingContainerId = response2Lines.get(1);

        assertThat(existingImageId, is(originalImageId));
        assertThat(existingContainerId, is(originalContainerId));
    }

    @Test
    @DisplayName("Given an image that builds and runs a container, " +
            "and we specify to not use the docker cache, " +
            "when it's run twice, " +
            "then the docker cache from the first run should not be available to the second run."
    )
    public void buildAndRunNestedDockerContainerNoCache() {
        Response response = given()
                .basePath(JOB_RUN_PATH)
                .queryParam("image", "scottg489/docker-test-support-job:latest")
                .queryParam("use_docker_cache", "false")
                .get();
        response.then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN);

        List<String> responseLines = response.asString().lines().collect(Collectors.toList());
        String originalImageId = responseLines.get(0);
        String originalContainerId = responseLines.get(1);

        Response response2 = given()
                .get(JOB_RUN_PATH + "?image=scottg489/docker-test-support-job:latest");
        response2.then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.TEXT_PLAIN);

        List<String> response2Lines = response2.asString().lines().collect(Collectors.toList());
        String existingImageId = response2Lines.get(0);
        String existingContainerId = response2Lines.get(1);

        assertThat(existingImageId, is(originalImageId));
        assertThat(existingContainerId, is(not(originalContainerId)));
    }


    private void cleanUpVolumes(String volumeId) {
        String adminBaseUri = getFromConfig("adminBaseUri");
        String adminUsername = getFromConfig("adminUsername");
        String adminPassword = getFromConfig("adminPassword");

        given()
                .baseUri(adminBaseUri)
                .auth().basic(adminUsername, adminPassword)
                .queryParam("id", volumeId)
                .post(DOCKER_VOLUME_RM_TASK_PATH)
        .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
