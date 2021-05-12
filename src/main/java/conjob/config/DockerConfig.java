package conjob.config;

import conjob.core.job.DockerAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DockerConfig {
    private String username;
    private String password;
    private DockerAdapter.Runtime containerRuntime;
}
