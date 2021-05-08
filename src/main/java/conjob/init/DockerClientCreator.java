package conjob.init;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;

import java.util.Objects;

public class DockerClientCreator {
    private final AuthedDockerClientCreator authedDockerClientCreator;
    private final DefaultDockerClient.Builder dockerClientBuilder;

    public DockerClientCreator(
            DefaultDockerClient.Builder dockerClientBuilder,
            AuthedDockerClientCreator authedDockerClientCreator) {
        this.dockerClientBuilder = dockerClientBuilder;
        this.authedDockerClientCreator = authedDockerClientCreator;
    }

    public DockerClient createDockerClient(String username, String password) throws DockerException, InterruptedException {
        DefaultDockerClient docker;
        if (Objects.nonNull(username) && Objects.nonNull(password)) {
            docker = authedDockerClientCreator.createDockerClient(username, password);
        } else {
            docker = dockerClientBuilder.build();
        }

        return docker;
    }
}