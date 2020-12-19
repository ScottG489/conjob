package conjob.service;

import com.spotify.docker.client.exceptions.DockerException;

import java.util.Optional;

public class SecretStore {
    private final DockerAdapter dockerAdapter;

    public SecretStore(DockerAdapter dockerAdapter) {
        this.dockerAdapter = dockerAdapter;
    }

    public Optional<String> findSecret(String secretName) throws SecretStoreException {
        try {
            return dockerAdapter.listAllVolumeNames().stream()
                    .filter(volName -> volName.equals(secretName))
                    .findFirst();
        } catch (DockerException | InterruptedException e) {
            throw new SecretStoreException(e);
        }
    }
}
