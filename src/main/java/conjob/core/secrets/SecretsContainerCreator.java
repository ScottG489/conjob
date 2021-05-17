package conjob.core.secrets;

import conjob.core.secrets.exception.CreateSecretsContainerException;
import conjob.core.secrets.model.SecretsConfig;

public class SecretsContainerCreator {
    private final SecretsDockerAdapter secretsAdapter;

    public SecretsContainerCreator(SecretsDockerAdapter secretsAdapter) {
        this.secretsAdapter = secretsAdapter;
    }

    public String createIntermediaryContainer(SecretsConfig secretsConfig) {
        String containerId;
        try {
            containerId = secretsAdapter.createVolumeCreatorContainer(secretsConfig);
        } catch (CreateSecretsContainerException e) {
            secretsAdapter.pullImage(secretsConfig.getIntermediaryContainerImage());
            containerId = secretsAdapter.createVolumeCreatorContainer(secretsConfig);
        }
        return containerId;
    }
}