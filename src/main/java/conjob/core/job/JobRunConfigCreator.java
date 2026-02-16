package conjob.core.job;

import conjob.core.job.model.JobRunConfig;

public class JobRunConfigCreator {
    public JobRunConfig getContainerConfig(String imageName, String input, String dockerCacheVolumeName, String secretsVolumeName, boolean useDockerCache) {
        JobRunConfig jobRunConfig;
        if (input != null && !input.isEmpty()) {
            jobRunConfig = new JobRunConfig(imageName, input, dockerCacheVolumeName, secretsVolumeName, useDockerCache);
        } else {
            jobRunConfig = new JobRunConfig(imageName, null, dockerCacheVolumeName, secretsVolumeName, useDockerCache);
        }

        return jobRunConfig;
    }
}
