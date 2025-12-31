package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.RunJobException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterStartContainerTest {
    private static final String TEST_SUPPORT_CONTAINER = "scottg489/test-support-job:latest";
    private static DockerClient dockerClient;
    private DockerAdapter dockerAdapter;
    private String containerId;

    @BeforeContainer
    static void beforeAll() throws InterruptedException {
        dockerClient = DockerClientFactory.createDefaultClient();
        dockerClient.pullImageCmd(TEST_SUPPORT_CONTAINER).start().awaitCompletion();
    }

    @AfterContainer
    static void afterAll() {
        dockerClient.removeImageCmd(TEST_SUPPORT_CONTAINER).withForce(true).exec();
    }

    @BeforeTry
    void beforeEach() {
        dockerAdapter = new DockerAdapter(dockerClient);
    }

    @AfterTry
    void afterEach() {
        if (containerId != null && !containerId.isBlank()) dockerClient.removeContainerCmd(containerId).exec();
    }

    @Property(tries = 10)
    @Label("Given a created container, " +
            "when starting that container, " +
            "and it exits, " +
            "should return the code it exited with.")
    void startContainerThenWaitForExit(@ForAll @LongRange(max = 255) long givenExitCode)
            throws RunJobException {
        containerId = dockerClient.createContainerCmd(TEST_SUPPORT_CONTAINER)
                .withCmd("0|||" + givenExitCode)
                .exec()
                .getId();
        Long exitStatusCode = dockerAdapter.startContainerThenWaitForExit(containerId);

        assertThat(exitStatusCode, is(givenExitCode));
    }

    @Property(tries = 100)
    @Label("Given a nonexistent container id, " +
            "when starting that container, " +
            "should throw a RunJobException.")
    void startContainerDockerException(@ForAll("wellFormedContainerId") String givenContainerId) {
        assertThrows(RunJobException.class, () -> dockerAdapter.startContainerThenWaitForExit(givenContainerId));
    }

    @Provide
    Arbitrary<String> wellFormedContainerId() {
        return Arbitraries.strings().withCharRange('a', 'f').numeric().ofLength(64);
    }
}
