package conjob.init;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerClientCreatorFactory {
    private final DefaultDockerClientConfig.Builder configBuilder;

    public DockerClientCreatorFactory(DefaultDockerClientConfig.Builder configBuilder) {
        this.configBuilder = configBuilder;
    }

    public DockerClientCreator create(String username, String password) {
        if (username != null && password != null) {
            DockerClientConfig config = configBuilder
                    .withRegistryUsername(username)
                    .withRegistryPassword(password)
                    .build();
            AuthConfig authConfig = new AuthConfig()
                    .withUsername(username)
                    .withPassword(password);
            return new AuthedDockerClientCreator(config, authConfig);
        }

        return new DefaultDockerClientCreator(configBuilder.build());
    }
}
