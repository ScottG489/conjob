package conjob.init;

import com.github.dockerjava.api.DockerClient;

public interface DockerClientCreator {
    DockerClient createDockerClient();
}
