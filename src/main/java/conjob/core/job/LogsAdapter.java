package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;

// You would mock this class. So it's fine if it's constructor takes a 3rd party class
//   because the mocking framework won't require us to call it's constructor. However, for
//   all the functions on the mock we should take and return non-third party objects. That way
//   we're always dealing with classes that we own.
public class LogsAdapter {
    private final DockerClient dockerClient;

    public LogsAdapter(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String readAllLogsUntilExit(String containerId) throws DockerException, InterruptedException {

        LogStream logs = dockerClient.logs(
                containerId,
                DockerClient.LogsParam.stdout(),
                DockerClient.LogsParam.stderr(),
                DockerClient.LogsParam.follow());

        return logs.readFully();
    }
}
