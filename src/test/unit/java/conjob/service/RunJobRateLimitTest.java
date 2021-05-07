package conjob.service;

import conjob.config.JobConfig;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RunJobRateLimitTest {

    @Property
    @Label("Given job count limiter, " +
            "and it has no limits, " +
            "when checking if it's at the limit, " +
            "then it should always be false.")
    void noLimit(@ForAll("noLimit") RunJobRateLimit jobCountLimiter) {
        assertThat(jobCountLimiter.isAtLimit(), is(false));
    }

    @Test
    @DisplayName("Given job count limiter, " +
            "and it is always at the limit, " +
            "when checking if it's at the limit, " +
            "then it should always be true.")
    void noRunsAllowedConfig() {
        Long max = Long.MAX_VALUE;
        JobConfig.LimitConfig noRunsAllowedConfig =
                new JobConfig.LimitConfig(0L, max, max, max);

        boolean atLimit = new RunJobRateLimit(noRunsAllowedConfig).isAtLimit();

        assertThat(atLimit, is(true));
    }

    @Provide
    Arbitrary<RunJobRateLimit> noLimit() {
        Long max = Long.MAX_VALUE;
        Arbitrary<JobConfig.LimitConfig> noLimitsConfig = Arbitraries.just(
                new JobConfig.LimitConfig(max, max, max, max));
        return noLimitsConfig.map(RunJobRateLimit::new);
    }
}