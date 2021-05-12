package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Volume;
import conjob.core.job.exception.*;
import conjob.core.job.model.JobRunConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Create more specific exceptions for when ImageNotFoundException is thrown
public class DockerAdapter {
    private static final String RUNTIME = "sysbox-runc";
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String SECRETS_VOLUME_MOUNT_OPTIONS = "ro";

    private final DockerClient dockerClient;
    private final Runtime containerRuntime;

    public DockerAdapter(DockerClient dockerClient) {
        this(dockerClient, Runtime.SYSBOX_RUNC);
    }

    public DockerAdapter(DockerClient dockerClient, Runtime containerRuntime) {
        this.dockerClient = dockerClient;
        this.containerRuntime = containerRuntime;
    }

    public List<String> listAllVolumeNames() throws DockerException, InterruptedException {
        return dockerClient.listVolumes().volumes().stream()
                .map(Volume::name).collect(Collectors.toList());
    }

    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        HostConfig hostConfig = getHostConfig(jobRunConfig.getSecretsVolumeName());

        ContainerConfig containerConfig = getContainerConfig(
                jobRunConfig.getJobName(),
                jobRunConfig.getInput(),
                hostConfig);

        try {
            return dockerClient.createContainer(containerConfig).id();
        } catch (DockerException | InterruptedException e) {
            throw new CreateJobRunException(e);
        }
    }

    public void pullImage(String imageName) throws JobUpdateException {
        try {
            dockerClient.pull(imageName);
        } catch (DockerException | InterruptedException e) {
            throw new JobUpdateException(e);
        }
    }

    public Long startContainerThenWaitForExit(String containerId) throws RunJobException {
        try {
            dockerClient.startContainer(containerId);
            return dockerClient.waitContainer(containerId).statusCode();
        } catch (DockerException | InterruptedException e) {
            throw new RunJobException(e);
        }
    }

    public Long stopContainer(String containerId, int killTimeoutSeconds) throws StopJobRunException {
        try {
            dockerClient.stopContainer(containerId, killTimeoutSeconds);
            return dockerClient.waitContainer(containerId).statusCode();
        } catch (DockerException | InterruptedException e) {
            throw new StopJobRunException(e);
        }
    }

    // TODO: There seems to be an issue with reading logs where if you read them too quickly,
    // TODO:   before any output has been produced, then the read will finish and return an empty
    // TODO:   string when really it should have waited for the job to finish. Not sure why this is.
    public String readAllLogsUntilExit(String containerId) throws ReadLogsException {
        LogStream logs;
        try {
            logs = dockerClient.logs(
                    containerId,
                    DockerClient.LogsParam.stdout(),
                    DockerClient.LogsParam.stderr(),
                    DockerClient.LogsParam.follow());
            return logs.readFully();
        } catch (DockerException | InterruptedException e) {
            throw new ReadLogsException(e);
        }
    }

    private ContainerConfig getContainerConfig(String jobName, String input, HostConfig hostConfig) {
        ContainerConfig.Builder containerConfigBuilder = ContainerConfig.builder()
                .image(jobName)
                .hostConfig(hostConfig);

        if (input != null) {
            containerConfigBuilder.cmd(input);
        }

        return containerConfigBuilder.build();
    }

    private HostConfig getHostConfig(String secretsVolumeName) {
        HostConfig.Builder hostConfigBuilder = getHostConfigBuilderFor(containerRuntime);
        if (secretsVolumeName != null) {
            hostConfigBuilder.appendBinds(
                    secretsVolumeName
                            + ":" + SECRETS_VOLUME_MOUNT_PATH
                            + ":" + SECRETS_VOLUME_MOUNT_OPTIONS);
        }
        return hostConfigBuilder.build();
    }

    private HostConfig.Builder getHostConfigBuilderFor(Runtime runtime) {
        return Map.of(Runtime.DEFAULT, HostConfig.builder(),
                Runtime.SYSBOX_RUNC, HostConfig.builder().runtime(RUNTIME)
        ).get(runtime);
    }

    public enum Runtime {
        DEFAULT, SYSBOX_RUNC
    }
}
