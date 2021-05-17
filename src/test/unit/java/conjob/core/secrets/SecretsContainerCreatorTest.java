package conjob.core.secrets;

import conjob.core.secrets.SecretsContainerCreator;
import conjob.core.secrets.SecretsDockerAdapter;
import conjob.core.secrets.exception.CreateSecretsContainerException;
import conjob.core.secrets.exception.UpdateSecretsImageException;
import conjob.core.secrets.model.SecretsConfig;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SecretsContainerCreatorTest {
    private SecretsContainerCreator secretsContainerCreator;
    private SecretsDockerAdapter mockSecretsAdapter;

    @BeforeTry
    void beforeEach() {
        mockSecretsAdapter = mock(SecretsDockerAdapter.class);
        secretsContainerCreator = new SecretsContainerCreator(mockSecretsAdapter);
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "and container id, " +
            "when creating the container, " +
            "should return id of created container.")
    void createIntermediaryContainer(
            @ForAll("secretsConfig") SecretsConfig givenSecretsConfig,
            @ForAll String givenContainerId) {
        when(mockSecretsAdapter.createVolumeCreatorContainer(givenSecretsConfig))
                .thenReturn(givenContainerId);

        String containerId = secretsContainerCreator.createIntermediaryContainer(givenSecretsConfig);

        assertThat(containerId, is(givenContainerId));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating the container, " +
            "and it fails, " +
            "and then it succeeds, " +
            "should pull the intermediary image, " +
            "and return id of created container.")
    void containerCreationFailsThenSucceeds(
            @ForAll("secretsConfig") SecretsConfig givenSecretsConfig,
            @ForAll String givenContainerId) {
        when(mockSecretsAdapter.createVolumeCreatorContainer(givenSecretsConfig))
                .thenThrow(CreateSecretsContainerException.class)
                .thenReturn(givenContainerId);

        String containerId = secretsContainerCreator.createIntermediaryContainer(givenSecretsConfig);

        assertThat(containerId, is(givenContainerId));
        verify(mockSecretsAdapter, times(1))
                .pullImage(givenSecretsConfig.getIntermediaryContainerImage());
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating the container, " +
            "and it fails, " +
            "and then it fails again after pull, " +
            "should throw the expected exception.")
    void containerCreationFailsEvenAfterPull(
            @ForAll("secretsConfig") SecretsConfig givenSecretsConfig) {
        when(mockSecretsAdapter.createVolumeCreatorContainer(givenSecretsConfig))
                .thenThrow(CreateSecretsContainerException.class)
                .thenThrow(CreateSecretsContainerException.class);

        assertThrows(CreateSecretsContainerException.class, () ->
                secretsContainerCreator.createIntermediaryContainer(givenSecretsConfig));
    }

    @Property
    @Label("Given a SecretsConfig, " +
            "when creating the container, " +
            "and it fails, " +
            "and then the pull fails, " +
            "should throw the expected exception.")
    void containerCreationFailsThenPullFails(
            @ForAll("secretsConfig") SecretsConfig givenSecretsConfig) {
        when(mockSecretsAdapter.createVolumeCreatorContainer(givenSecretsConfig))
                .thenThrow(CreateSecretsContainerException.class);
        doThrow(UpdateSecretsImageException.class).when(mockSecretsAdapter)
                .pullImage(givenSecretsConfig.getIntermediaryContainerImage());

        assertThrows(UpdateSecretsImageException.class, () ->
                secretsContainerCreator.createIntermediaryContainer(givenSecretsConfig));
    }

    @Provide
    Arbitrary<SecretsConfig> secretsConfig() {
        return Arbitraries.forType(SecretsConfig.class);
    }
}