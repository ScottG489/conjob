package conjob.healthcheck;

import com.codahale.metrics.health.HealthCheck;

import java.io.InputStream;
import java.util.Properties;

public class VersionCheck extends HealthCheck {
    @Override
    protected Result check() throws Exception {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = loader.getResourceAsStream("version.properties");
        Properties properties = new Properties();
        properties.load(resourceStream);
        String version = properties.getProperty("version");
        return Result.healthy(version);
    }
}
