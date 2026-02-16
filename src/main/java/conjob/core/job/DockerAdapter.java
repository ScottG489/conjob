package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import conjob.core.job.exception.*;
import conjob.core.job.model.JobRunConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Create more specific exceptions for when ImageNotFoundException is thrown
public class DockerAdapter {
    private static final String RUNTIME = "sysbox-runc";
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";

    private final DockerClient dockerClient;
    private final Runtime containerRuntime;

    public DockerAdapter(DockerClient dockerClient) {
        this(dockerClient, Runtime.SYSBOX_RUNC);
    }

    public DockerAdapter(DockerClient dockerClient, Runtime containerRuntime) {
        this.dockerClient = dockerClient;
        this.containerRuntime = containerRuntime;
    }

    public List<String> listAllVolumeNames() {
        return dockerClient.listVolumesCmd().exec().getVolumes().stream()
                .map(InspectVolumeResponse::getName).collect(Collectors.toList());
    }

    public String createJobRun(JobRunConfig jobRunConfig) throws CreateJobRunException {
        HostConfig hostConfig = getHostConfig(jobRunConfig.getDockerCacheVolumeName(), jobRunConfig.getSecretsVolumeName(), jobRunConfig.isUseDockerCache());

        try {
            var createCmd = dockerClient.createContainerCmd(jobRunConfig.getJobName())
                    .withHostConfig(hostConfig);

            if (jobRunConfig.getInput() != null) {
                createCmd.withCmd(jobRunConfig.getInput());
            }

            CreateContainerResponse response = createCmd.exec();
            return response.getId();
        } catch (Exception e) {
            throw new CreateJobRunException(e);
        }
    }

    public void pullImage(String imageName) throws JobUpdateException {
        try {
            dockerClient.pullImageCmd(imageName).start().awaitCompletion();
        } catch (Exception e) {
            throw new JobUpdateException(e);
        }
    }

    public Long startContainerThenWaitForExit(String containerId) throws RunJobException {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            return (long) dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode();
        } catch (Exception e) {
            throw new RunJobException(e);
        }
    }

    public Long stopContainer(String containerId, int killTimeoutSeconds) throws StopJobRunException {
        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(killTimeoutSeconds).exec();
        } catch (com.github.dockerjava.api.exception.NotModifiedException e) {
            // Container already stopped, this is fine
        } catch (Exception e) {
            throw new StopJobRunException(e);
        }

        try {
            return (long) dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode();
        } catch (Exception e) {
            throw new StopJobRunException(e);
        }
    }

    // TODO: There seems to be an issue with reading logs where if you read them too quickly,
    // TODO:   before any output has been produced, then the read will finish and return an empty
    // TODO:   string when really it should have waited for the job to finish. Not sure why this is.
    public String readAllLogsUntilExit(String containerId) throws ReadLogsException {
        try {
            StringBuilder logs = new StringBuilder();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new LogContainerResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            logs.append(new String(frame.getPayload()));
                        }
                    }).awaitCompletion();
            return logs.toString();
        } catch (Exception e) {
            throw new ReadLogsException(e);
        }
    }

    public void removeVolume(String volumeId) {
        try {
            dockerClient.removeVolumeCmd(volumeId).exec();
        } catch (Exception e) {
            throw new RemoveVolumeException(e);
        }
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec();
        } catch (Exception e) {
            throw new RemoveContainerException(e);
        }
    }

    private HostConfig getHostConfig(String dockerCacheVolumeName, String secretsVolumeName, boolean useDockerCache) {
        HostConfig hostConfig = new HostConfig();

        if (containerRuntime == Runtime.SYSBOX_RUNC) {
            hostConfig.withRuntime(RUNTIME);
        }

        if (useDockerCache) {
            Bind dockerBind = new Bind(dockerCacheVolumeName, new Volume("/var/lib/docker"));
            hostConfig.withBinds(dockerBind);

            if (secretsVolumeName != null) {
                Bind secretsBind = new Bind(secretsVolumeName,
                        new Volume(SECRETS_VOLUME_MOUNT_PATH),
                        AccessMode.ro);
                hostConfig.withBinds(dockerBind, secretsBind);
            }
        } else if (secretsVolumeName != null) {
            Bind secretsBind = new Bind(secretsVolumeName,
                    new Volume(SECRETS_VOLUME_MOUNT_PATH),
                    AccessMode.ro);
            hostConfig.withBinds(secretsBind);
        }

        return hostConfig;
    }

    public enum Runtime {
        DEFAULT, SYSBOX_RUNC
    }
}
