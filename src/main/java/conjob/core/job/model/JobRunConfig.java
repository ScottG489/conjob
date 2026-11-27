package conjob.core.job.model;


import lombok.Value;

@Value
public class JobRunConfig {
    String jobName;
    String input;
    String dockerCacheVolumeName;
    String secretsVolumeName;
    boolean useDockerCache;
    // Recorded on the container as labels, so leftovers can be cleaned up later.
    boolean remove;
    boolean removeImage;
}
