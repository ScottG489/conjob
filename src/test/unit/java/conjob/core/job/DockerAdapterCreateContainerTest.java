package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerCreation;
import conjob.core.job.exception.CreateJobRunException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerAdapterCreateContainerTest {
    private DockerAdapter dockerAdapter;
    private DockerClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(DockerClient.class);
        dockerAdapter = new DockerAdapter(mockClient);
    }

    @Test
    @DisplayName("Given a JobRunConfig," +
            "when creating a job run," +
            "should return id of container creation")
    void createJobRun() throws CreateJobRunException, DockerException, InterruptedException {
        String givenJobRunId = "given_id";
        ContainerCreation mockContainerCreation = mock(ContainerCreation.class);

        when(mockClient.createContainer(any())).thenReturn(mockContainerCreation);
        when(mockContainerCreation.id()).thenReturn(givenJobRunId);

        JobRunConfig jobRunConfig = new JobRunConfig(
                "job_name",
                "input",
                "secret_volume_name"
        );
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
    }

    @Test
    @DisplayName("Given a JobRunConfig with no input," +
            "when creating a job run," +
            "should return id of container creation")
    void createJobRunWithNoInput() throws CreateJobRunException, DockerException, InterruptedException {
        String givenJobRunId = "given_id";
        ContainerCreation mockContainerCreation = mock(ContainerCreation.class);

        when(mockClient.createContainer(any())).thenReturn(mockContainerCreation);
        when(mockContainerCreation.id()).thenReturn(givenJobRunId);

        JobRunConfig jobRunConfig = new JobRunConfig(
                "job_name",
                null,
                "secret_volume_name"
        );
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
    }

    @Test
    @DisplayName("Given a JobRunConfig with no input," +
            "when creating a job run," +
            "should return id of container creation")
    void createJobRunWithNoSecretVolumeName() throws CreateJobRunException, DockerException, InterruptedException {
        String givenJobRunId = "given_id";
        ContainerCreation mockContainerCreation = mock(ContainerCreation.class);

        when(mockClient.createContainer(any())).thenReturn(mockContainerCreation);
        when(mockContainerCreation.id()).thenReturn(givenJobRunId);

        JobRunConfig jobRunConfig = new JobRunConfig(
                "job_name",
                "input",
                null
        );
        String jobRunId = dockerAdapter.createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
    }

    @Test
    @DisplayName("Given a job config," +
            "when a DockerException is thrown," +
            "should throw a CreateJobException")
    void createJobRunDockerException() throws DockerException, InterruptedException {
        when(mockClient.createContainer(any())).thenThrow(new DockerException(""));

        JobRunConfig jobRunConfig = new JobRunConfig(
                "job_name",
                "input",
                "secret_volume_name"
        );

        assertThrows(CreateJobRunException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }

    @Test
    @DisplayName("Given a job config," +
            "when an InterruptedException is thrown," +
            "should throw a CreateJobException")
    void createJobRunInterruptedException() throws DockerException, InterruptedException {
        when(mockClient.createContainer(any())).thenThrow(new InterruptedException());

        JobRunConfig jobRunConfig = new JobRunConfig(
                "job_name",
                "input",
                "secret_volume_name"
        );

        assertThrows(CreateJobRunException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }

    @Test
    @DisplayName("Given a job config," +
            "when a unexpected Exception is thrown," +
            "should throw that exception")
    void createJobRunUnexpectedException() throws DockerException, InterruptedException {
        when(mockClient.createContainer(any())).thenThrow(new RuntimeException());

        JobRunConfig jobRunConfig = new JobRunConfig(
                "job_name",
                "input",
                "secret_volume_name"
        );

        assertThrows(RuntimeException.class, () -> dockerAdapter.createJobRun(jobRunConfig));
    }
}