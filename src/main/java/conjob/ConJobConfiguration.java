package conjob;

import conjob.config.ConJobConfig;
import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConJobConfiguration extends Configuration {
    private ConJobConfig conjob;
}
