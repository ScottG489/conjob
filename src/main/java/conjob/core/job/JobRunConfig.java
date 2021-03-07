package conjob.core.job;


import lombok.Value;

@Value
public class JobRunConfig {
    String jobName;
    String input;
    String secretsVolumeName;
}
