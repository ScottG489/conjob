package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import java.util.List;

// Docker won't remove an image while a container uses it, so tests that remove an image first
// remove its containers, including ones that other test classes left behind.
class TestImageContainers {
    static void removeAll(DockerClient dockerClient, String image) {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withAncestorFilter(List.of(image))
                    .exec();
            for (Container container : containers) {
                try {
                    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
