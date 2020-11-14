package conjob.healthcheck;

import com.codahale.metrics.health.HealthCheck;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VersionCheckTest {

    @org.junit.Test
    public void check() throws Exception {
        VersionCheck versionCheck = new VersionCheck();
        HealthCheck.Result checkResult = versionCheck.check();

        assertThat(checkResult.isHealthy(), is(true));
        assertThat(checkResult.getMessage(), is("1.2.3"));
    }
}
