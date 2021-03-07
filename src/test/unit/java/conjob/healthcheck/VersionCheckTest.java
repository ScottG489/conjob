package conjob.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class VersionCheckTest {
    @Test
    // TODO: This test isn't actually doing what is described here
    @DisplayName("Version health check should return value specified in version.properties")
    public void check() throws Exception {
        VersionCheck versionCheck = new VersionCheck();
        HealthCheck.Result checkResult = versionCheck.check();

        assertThat(checkResult.isHealthy(), is(true));
        assertThat(checkResult.getMessage(), is("1.2.3"));
    }
}
