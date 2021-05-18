package conjob.core.secrets;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import conjob.core.secrets.exception.CreateSecretsContainerException;
import conjob.core.secrets.model.SecretsConfig;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.UseType;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecretsDockerAdapterCreateVolumeContainerTest {

    private DockerClient mockClient;
    private SecretsDockerAdapter secretsAdapter;

    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        secretsAdapter = new SecretsDockerAdapter(mockClient);
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating a volume creator container, " +
            "should return id of container.")
    void createVolumeCreatorContainer(
            @ForAll @UseType SecretsConfig secretsConfig,
            @ForAll String givenContainerId) throws DockerException, InterruptedException {
        when(mockClient.createContainer(
                any(ContainerConfig.class),
                eq(secretsConfig.getIntermediaryContainerName()))
                .id())
                .thenReturn(givenContainerId);

        String containerId = secretsAdapter.createVolumeCreatorContainer(secretsConfig);

        assertThat(containerId, is(givenContainerId));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating a volume creator container, " +
            "and a DockerException is thrown, " +
            "should throw a CreateSecretsContainerException.")
    void catchDockerException(
            @ForAll @UseType SecretsConfig secretsConfig) throws DockerException, InterruptedException {
        when(mockClient.createContainer(
                any(ContainerConfig.class),
                eq(secretsConfig.getIntermediaryContainerName())))
                .thenThrow(DockerException.class);

        assertThrows(CreateSecretsContainerException.class, () -> secretsAdapter.createVolumeCreatorContainer(secretsConfig));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating a volume creator container, " +
            "and a InterruptedException is thrown, " +
            "should throw a CreateSecretsContainerException.")
    void catchInterruptedException(
            @ForAll @UseType SecretsConfig secretsConfig) throws DockerException, InterruptedException {
        when(mockClient.createContainer(
                any(ContainerConfig.class),
                eq(secretsConfig.getIntermediaryContainerName())))
                .thenThrow(InterruptedException.class);

        assertThrows(CreateSecretsContainerException.class, () -> secretsAdapter.createVolumeCreatorContainer(secretsConfig));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating a volume creator container, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void catchUnexpectedException(
            @ForAll @UseType SecretsConfig secretsConfig) throws DockerException, InterruptedException {
        when(mockClient.createContainer(
                any(ContainerConfig.class),
                eq(secretsConfig.getIntermediaryContainerName())))
                .thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> secretsAdapter.createVolumeCreatorContainer(secretsConfig));
    }
}