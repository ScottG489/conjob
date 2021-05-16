package conjob.core.job.model;

import lombok.Value;

@Value
public class SecretsConfig {
    String secretsVolumeName;
    String destinationPath;
    String intermediaryContainerImage;
    String intermediaryContainerName;
}
