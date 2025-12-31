package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class AuthedDockerClientCreator {
    private final DockerClientConfig baseConfig;
    private final AuthConfig authConfig;

    public AuthedDockerClientCreator(
            DockerClientConfig baseConfig,
            AuthConfig authConfig) {
        this.baseConfig = baseConfig;
        this.authConfig = authConfig;
    }

    public DockerClient createDockerClient(String username, String password) {
        AuthConfig updatedAuthConfig = this.authConfig
                .withUsername(username)
                .withPassword(password);

        DockerClientConfig configWithAuth = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(baseConfig.getDockerHost().toString())
                .withRegistryUsername(username)
                .withRegistryPassword(password)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(configWithAuth.getDockerHost())
                .build();

        DockerClient docker = DockerClientImpl.getInstance(configWithAuth, httpClient);
        validateCredentials(docker, updatedAuthConfig);
        return docker;
    }

    private void validateCredentials(DockerClient docker, AuthConfig authConfig) {
        try {
            docker.authCmd().withAuthConfig(authConfig).exec();
        } catch (Exception e) {
            throw new RuntimeException("Incorrect docker credentials", e);
        }
    }
}
