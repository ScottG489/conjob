package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerCreation;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerAdapterCreateContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Property
    @Label("Given a JobRunConfig, " +
            "when creating a job run, " +
            "should return id of container creation.")
    void createJobRun(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String secretVolumeName,
            @ForAll String givenJobRunId
    ) throws CreateJobRunException, DockerException, InterruptedException {
        ContainerCreation mockContainerCreation = mock(ContainerCreation.class);

        when(mockClient.createContainer(any())).thenReturn(mockContainerCreation);
        when(mockContainerCreation.id()).thenReturn(givenJobRunId);

        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretVolumeName);
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
    }

    @Property
    @Label("Given a job config, " +
            "when a DockerException is thrown, " +
            "should throw a CreateJobException.")
    void createJobRunDockerException(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String secretVolumeName
    ) throws DockerException, InterruptedException {
        when(mockClient.createContainer(any())).thenThrow(new DockerException(""));

        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretVolumeName);

        assertThrows(CreateJobRunException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }

    @Property
    @Label("Given a job config, " +
            "when an InterruptedException is thrown, " +
            "should throw a CreateJobException.")
    void createJobRunInterruptedException(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String secretVolumeName
    ) throws DockerException, InterruptedException {
        when(mockClient.createContainer(any())).thenThrow(new InterruptedException());

        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretVolumeName);

        assertThrows(CreateJobRunException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }

    @Property
    @Label("Given a job config, " +
            "when a unexpected Exception is thrown, " +
            "should throw that exception.")
    void createJobRunUnexpectedException(
            @ForAll String jobName,
            @ForAll @WithNull String input,
            @ForAll @WithNull String secretVolumeName
    ) throws DockerException, InterruptedException {
        when(mockClient.createContainer(any())).thenThrow(new RuntimeException());

        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretVolumeName);

        assertThrows(RuntimeException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }
}