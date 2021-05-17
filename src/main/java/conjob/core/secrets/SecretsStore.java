package conjob.core.secrets;

import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.DockerAdapter;

import java.util.Optional;

public class SecretsStore {
    private final DockerAdapter dockerAdapter;

    public SecretsStore(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public Optional<String> findSecrets(String secretsVolumeName) throws SecretsStoreException {
        try {
            return dockerAdapter.listAllVolumeNames().stream()
                    .filter(volName -> volName.equals(secretsVolumeName))
                    .findFirst();
        } catch (DockerException | InterruptedException e) {
            throw new SecretsStoreException(e);
        }
    }
}
