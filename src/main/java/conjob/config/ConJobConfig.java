package conjob.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConJobConfig {
    private DockerConfig docker;
    private AdminConfig admin;
    private AuthConfig auth;
    private JobConfig job;
}
