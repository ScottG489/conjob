package conjob.service;

import conjob.core.job.SecretsDockerAdapter;
import conjob.core.job.exception.CreateSecretsContainerException;
import conjob.core.job.model.SecretsConfig;

public class SecretsContainerCreator {
    private final SecretsDockerAdapter secretsAdapter;

    public SecretsContainerCreator(SecretsDockerAdapter secretsAdapter) {
        this.secretsAdapter = secretsAdapter;
    }

    String createIntermediaryContainer(SecretsConfig secretsConfig) {
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