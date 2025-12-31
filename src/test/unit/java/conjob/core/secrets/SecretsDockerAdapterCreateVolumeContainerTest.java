package conjob.core.secrets;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
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
            @ForAll String givenContainerId)  {
        CreateContainerResponse mockResponse = mock(CreateContainerResponse.class);
        when(mockClient.createContainerCmd(secretsConfig.getIntermediaryContainerImage())
                .withName(secretsConfig.getIntermediaryContainerName())
                .withHostConfig(any())
                .exec())
                .thenReturn(mockResponse);
        when(mockResponse.getId()).thenReturn(givenContainerId);

        String containerId = secretsAdapter.createVolumeCreatorContainer(secretsConfig);

        assertThat(containerId, is(givenContainerId));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating a volume creator container, " +
            "and an Exception is thrown, " +
            "should throw a CreateSecretsContainerException.")
    void catchException(
            @ForAll @UseType SecretsConfig secretsConfig)  {
        when(mockClient.createContainerCmd(secretsConfig.getIntermediaryContainerImage())
                .withName(secretsConfig.getIntermediaryContainerName())
                .withHostConfig(any())
                .exec())
                .thenThrow(RuntimeException.class);

        assertThrows(CreateSecretsContainerException.class, () -> secretsAdapter.createVolumeCreatorContainer(secretsConfig));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating a volume creator container, " +
            "and an unexpected Exception is thrown, " +
            "should throw that exception.")
    void catchUnexpectedException(
            @ForAll @UseType SecretsConfig secretsConfig)  {
        when(mockClient.createContainerCmd(secretsConfig.getIntermediaryContainerImage())
                .withName(secretsConfig.getIntermediaryContainerName())
                .withHostConfig(any())
                .exec())
                .thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> secretsAdapter.createVolumeCreatorContainer(secretsConfig));
    }
}