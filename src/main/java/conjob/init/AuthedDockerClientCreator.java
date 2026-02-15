package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class AuthedDockerClientCreator implements DockerClientCreator {
    private final DockerClientConfig config;
    private final AuthConfig authConfig;

    public AuthedDockerClientCreator(DockerClientConfig config, AuthConfig authConfig) {
        this.config = config;
        this.authConfig = authConfig;
    }

    public DockerClient createDockerClient() {
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        DockerClient docker = DockerClientImpl.getInstance(config, httpClient);
        validateCredentials(docker);
        return docker;
    }

    private void validateCredentials(DockerClient docker) {
        try {
            docker.authCmd().withAuthConfig(authConfig).exec();
        } catch (Exception e) {
            throw new RuntimeException("Incorrect docker credentials", e);
        }
    }
}
