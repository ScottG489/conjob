package conjob.core.job;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.WithNull;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DockerAdapterCreateContainerTest {
    private static DefaultDockerClient dockerClient;

    private DockerAdapter dockerAdapter;
    private String jobRunId;
    private String secretsVolumeName;

    @BeforeContainer
    static void beforeContainer() throws DockerCertificateException, DockerException, InterruptedException {
        dockerClient = DefaultDockerClient.fromEnv().build();
        dockerClient.pull("tianon/true:latest");
    }

    @AfterContainer
    static void afterContainer() throws DockerException, InterruptedException {
        dockerClient.removeImage("tianon/true", true, false);
    }

    @BeforeTry
    void setUp() {
        dockerAdapter = new DockerAdapter(dockerClient, DockerAdapter.Runtime.DEFAULT);
    }

    @AfterTry
    void tearDown() throws DockerException, InterruptedException {
        if (jobRunId != null && !jobRunId.isBlank()) dockerClient.removeContainer(jobRunId);
        if (secretsVolumeName != null && !secretsVolumeName.isBlank()) dockerClient.removeVolume(secretsVolumeName);
    }

    @Property(tries = 100)
    @Label("Given a JobRunConfig, " +
            "when creating a job run, " +
            "should return id of container creation.")
    void createJobRun(
            @ForAll("existingJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") String secretsVolumeName
    ) throws CreateJobRunException {
        this.secretsVolumeName = secretsVolumeName;
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName);
        jobRunId = dockerAdapter.createJobRun(jobRunConfig);

        assertThat(jobRunId, matchesPattern("[a-f0-9]{64}"));
    }

    @Property(tries = 100)
    @Label("Given a job config with a non-existent job, " +
            "when creating a job run, " +
            "should throw a CreateJobException.")
    void createJobRunNonExistantJob(
            @ForAll("nonexistantJob") String jobName,
            @ForAll @WithNull String input,
            @ForAll("dockerCacheVolumeName") String dockerCacheVolumeName,
            @ForAll("secretsVolumeName") @WithNull String secretsVolumeName) {
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, dockerCacheVolumeName,  secretsVolumeName);

        assertThrows(CreateJobRunException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }

    @Provide
    Arbitrary<String> existingJob() {
        return Arbitraries.just("tianon/true:latest");
    }

    @Provide
    Arbitrary<String> nonexistantJob() {
        return Arbitraries.just("local/job-that-does-not-exist-aeec82aa:latest");
    }

    @Provide
    Arbitrary<String> dockerCacheVolumeName() {
        Arbitrary<String> firstChar = Arbitraries.strings().alpha().numeric().ofLength(1);
        Arbitrary<String> lastChars = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(254);
        return Combinators.combine(firstChar, lastChars).as((first, last) -> first + last);
    }

    @Provide
    Arbitrary<String> secretsVolumeName() {
        Arbitrary<String> firstChar = Arbitraries.strings().alpha().numeric().ofLength(1);
        Arbitrary<String> lastChars = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(254);
        return Combinators.combine(firstChar, lastChars).as((first, last) -> first + last);
    }
}
