package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DefaultDockerClientCreator implements DockerClientCreator {
    private final DockerClientConfig config;

    public DefaultDockerClientCreator(DockerClientConfig config) {
        this.config = config;
    }

    public DockerClient createDockerClient() {
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
