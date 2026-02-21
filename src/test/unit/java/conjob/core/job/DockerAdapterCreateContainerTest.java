package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.WithNull;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DockerAdapterCreateContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a JobRunConfig, " +
            "when creating a job run, " +
            "should return id of container creation.")
    void createJobRun(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String dockerCacheVolumeName,
            @ForAll @WithNull String secretsVolumeName,
            @ForAll String givenJobRunId,
            @ForAll boolean remove
    ) throws CreateJobRunException {
        CreateContainerResponse mockResponse = mock(CreateContainerResponse.class);

        when(mockClient.createContainerCmd(jobName).withHostConfig(any()).exec()).thenReturn(mockResponse);
        when(mockResponse.getId()).thenReturn(givenJobRunId);

        JobRunConfig jobRunConfig =
                new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, true, remove);
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
    }

    @Property
    @Label("Given a job config, " +
            "when an Exception is thrown, " +
            "should throw a CreateJobRunException.")
    void createJobRunException(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String dockerCacheVolumeName,
            @ForAll @WithNull String secretsVolumeName,
            @ForAll boolean remove
    )  {
        when(mockClient.createContainerCmd(jobName).withHostConfig(any()).exec()).thenThrow(new RuntimeException(""));

        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, true, remove);

        assertThrows(CreateJobRunException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }

    @Property
    @Label("Given a job config, " +
            "when a unexpected Exception is thrown, " +
            "should throw that exception.")
    void createJobRunUnexpectedException(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String dockerCacheVolumeName,
            @ForAll @WithNull String secretsVolumeName,
            @ForAll boolean remove
    )  {
        when(mockClient.createContainerCmd(jobName).withHostConfig(any()).exec()).thenThrow(new RuntimeException());

        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, dockerCacheVolumeName, secretsVolumeName, true, remove);

        assertThrows(RuntimeException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }
}
