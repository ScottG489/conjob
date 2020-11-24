package conjob.config;

import lombok.Data;

@Data
public class ConJobConfig {
    private DockerConfig docker;
    private AdminConfig admin;
    private AuthConfig auth;
    private JobConfig job;
}
