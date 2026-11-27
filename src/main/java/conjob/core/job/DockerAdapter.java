package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import conjob.core.job.exception.*;
import conjob.core.job.model.ContainerImage;
import conjob.core.job.model.JobRunConfig;
import conjob.core.job.model.StoppedContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO: Create more specific exceptions for when ImageNotFoundException is thrown
public class DockerAdapter {
    private static final String RUNTIME = "sysbox-runc";
    private static final String SECRETS_VOLUME_MOUNT_PATH = "/run/build/secrets";
    private static final String REMOVE_LABEL = "conjob.remove";
    private static final String REMOVE_IMAGE_LABEL = "conjob.remove-image";

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
                    .withHostConfig(hostConfig)
                    .withLabels(Map.of(
                            REMOVE_LABEL, String.valueOf(jobRunConfig.isRemove()),
                            REMOVE_IMAGE_LABEL, String.valueOf(jobRunConfig.isRemoveImage())));

            if (jobRunConfig.getInput() != null && !jobRunConfig.getInput().isEmpty()) {
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

    public void startContainer(String containerId) throws RunJobException {
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } catch (Exception e) {
            throw new RunJobException(e);
        }
    }

    public Long waitForExit(String containerId) throws RunJobException {
        try {
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

    // Without force, Docker won't remove an image that a container uses, even a stopped one, or
    // remove an image by ID while it has more than one tag. Given a tag that isn't the image's
    // last, it removes just that tag.
    public void removeImage(String imageRef) {
        try {
            dockerClient.removeImageCmd(imageRef).exec();
        } catch (ConflictException e) {
            throw new ImageInUseException(e);
        } catch (Exception e) {
            throw new RemoveImageException(e);
        }
    }

    public Optional<String> findImageId(String imageRef) {
        return inspectImage(imageRef).map(InspectImageResponse::getId);
    }

    public Optional<List<String>> findImageTags(String imageRef) {
        return inspectImage(imageRef)
                .map(image -> image.getRepoTags() == null ? List.of() : image.getRepoTags());
    }

    private Optional<InspectImageResponse> inspectImage(String imageRef) {
        try {
            return Optional.of(dockerClient.inspectImageCmd(imageRef).exec());
        } catch (NotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new InspectImageException(e);
        }
    }

    // Containers of jobs run with remove=true that aren't running: they exited, never started,
    // or are dead.
    public List<StoppedContainer> listStoppedContainersToRemove() {
        try {
            return dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withLabelFilter(Map.of(REMOVE_LABEL, "true"))
                    .withStatusFilter(List.of("created", "exited", "dead"))
                    .exec().stream()
                    .map(container -> findStoppedContainer(container.getId()))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ListContainersException(e);
        }
    }

    // Empty if the container was removed since it was listed.
    private Optional<StoppedContainer> findStoppedContainer(String containerId) {
        InspectContainerResponse container;
        try {
            container = dockerClient.inspectContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            return Optional.empty();
        }
        // A container that never started has a zero finish time, so it stopped when it was created.
        Instant created = Instant.parse(container.getCreated());
        Instant finished = Instant.parse(container.getState().getFinishedAt());
        boolean removeImage = "true".equals(container.getConfig().getLabels().get(REMOVE_IMAGE_LABEL));
        return Optional.of(new StoppedContainer(containerId, removeImage, finished.isAfter(created) ? finished : created));
    }

    // Removes images that have no tags and that no container uses. Returns the bytes freed.
    public long pruneDanglingImages() {
        try {
            Long spaceReclaimed = dockerClient.pruneCmd(PruneType.IMAGES).withDangling(true).exec().getSpaceReclaimed();
            return spaceReclaimed == null ? 0 : spaceReclaimed;
        } catch (Exception e) {
            throw new RemoveImageException(e);
        }
    }

    public ContainerImage getContainerImage(String containerId) {
        try {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            return new ContainerImage(container.getImageId(), container.getConfig().getImage());
        } catch (Exception e) {
            throw new InspectContainerException(e);
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

        List<Bind> binds = new ArrayList<>();
        if (useDockerCache) {
            binds.add(new Bind(dockerCacheVolumeName, new Volume("/var/lib/docker")));
        }
        if (secretsVolumeName != null) {
            binds.add(new Bind(secretsVolumeName, new Volume(SECRETS_VOLUME_MOUNT_PATH), AccessMode.ro));
        }
        hostConfig.withBinds(binds);

        return hostConfig;
    }

    public enum Runtime {
        DEFAULT, SYSBOX_RUNC
    }
}
