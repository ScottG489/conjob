package conjob.core.job.model;


import lombok.Value;

@Value
public class JobRunConfig {
    String jobName;
    String input;
    String secretsVolumeName;
}
