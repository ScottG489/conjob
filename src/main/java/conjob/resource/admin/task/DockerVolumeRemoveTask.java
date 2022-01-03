package conjob.resource.admin.task;

import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.RemoveVolumeException;
import io.dropwizard.servlets.tasks.PostBodyTask;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerVolumeRemoveTask extends PostBodyTask {
    private final DockerAdapter dockerAdapter;

    public DockerVolumeRemoveTask(DockerAdapter dockerAdapter) {
        super("docker/volume/rm");
        this.dockerAdapter = dockerAdapter;
    }

    @Override
    public void execute(Map<String, List<String>> map, String s, PrintWriter printWriter) {
        for (String volumeId : map.getOrDefault("id", List.of())) {
            try {
                dockerAdapter.removeVolume(volumeId);
            } catch (RemoveVolumeException e) {
                // TODO: There's no way to force removing a volume with this lib, though it's supported by docker.
                // TODO:   Until we migrate to another docker lib that does this, we'll have to use this hack.
                removeContainersFromExceptionMessage(e.getCause().getCause().getMessage());
                map.getOrDefault("id", List.of()).forEach(dockerAdapter::removeVolume);
            }
        }
    }

    private void removeContainersFromExceptionMessage(String exceptionMessage) {
        Matcher matcher = Pattern.compile("\\[(.*)\\]").matcher(exceptionMessage);
        matcher.find();
        Arrays.stream(matcher.group(1).split(", ")).forEach(dockerAdapter::removeContainer);
    }
}
