package conjob.init;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.auth.FixedRegistryAuthSupplier;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import org.apache.http.HttpStatus;

public class AuthedDockerClientCreator {
    private final DefaultDockerClient.Builder dockerClientBuilder;
    private final RegistryAuth.Builder regAuthBuilder;

    public AuthedDockerClientCreator(
            DefaultDockerClient.Builder dockerClientBuilder,
            RegistryAuth.Builder regAuthBuilder) {
        this.dockerClientBuilder = dockerClientBuilder;
        this.regAuthBuilder = regAuthBuilder;
    }

    public DefaultDockerClient createDockerClient(String username, String password) throws DockerException, InterruptedException {
        DefaultDockerClient docker;
        RegistryAuth registryAuth = regAuthBuilder
                .username(username)
                .password(password)
                .build();
        docker = dockerClientBuilder.registryAuthSupplier(
                new FixedRegistryAuthSupplier(registryAuth, null))
                .build();
        validateCredentials(docker, registryAuth);
        return docker;
    }

    private void validateCredentials(DefaultDockerClient docker, RegistryAuth registryAuth) throws DockerException, InterruptedException {
        final int statusCode = docker.auth(registryAuth);
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("Incorrect docker credentials");
        }
    }
}