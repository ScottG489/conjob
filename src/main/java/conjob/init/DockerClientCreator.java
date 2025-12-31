package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.util.Objects;

public class DockerClientCreator {
    private final AuthedDockerClientCreator authedDockerClientCreator;
    private final DockerClientConfig dockerClientConfig;

    public DockerClientCreator(
            DockerClientConfig dockerClientConfig,
            AuthedDockerClientCreator authedDockerClientCreator) {
        this.dockerClientConfig = dockerClientConfig;
        this.authedDockerClientCreator = authedDockerClientCreator;
    }

    public DockerClient createDockerClient(String username, String password) {
        DockerClient docker;
        if (Objects.nonNull(username) && Objects.nonNull(password)) {
            docker = authedDockerClientCreator.createDockerClient(username, password);
        } else {
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(dockerClientConfig.getDockerHost())
                    .build();
            docker = DockerClientImpl.getInstance(dockerClientConfig, httpClient);
        }

        return docker;
    }
}