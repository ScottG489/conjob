package conjob.service;


import lombok.Value;

@Value
public class JobRunConfig {
    String jobName;
    String input;
    String secretsVolumeName;
}
