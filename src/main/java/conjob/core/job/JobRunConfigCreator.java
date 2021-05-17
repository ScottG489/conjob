package conjob.core.job;

import conjob.core.job.model.JobRunConfig;

public class JobRunConfigCreator {
    public JobRunConfig getContainerConfig(String imageName, String input, String secretsVolumeName) {
        JobRunConfig jobRunConfig;
        if (input != null && !input.isEmpty()) {
            jobRunConfig = new JobRunConfig(imageName, input, secretsVolumeName);
        } else {
            jobRunConfig = new JobRunConfig(imageName, null, secretsVolumeName);
        }

        return jobRunConfig;
    }
}
