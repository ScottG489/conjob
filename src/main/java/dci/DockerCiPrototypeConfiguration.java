package dci;

import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DockerCiPrototypeConfiguration extends Configuration {
    private String adminUsername;
    private String adminPassword;
}
