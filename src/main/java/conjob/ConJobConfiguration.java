package conjob;

import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConJobConfiguration extends Configuration {
    private String adminUsername;
    private String adminPassword;
    private String username;
    private String password;
}
