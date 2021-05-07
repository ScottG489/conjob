package conjob.service;

import conjob.config.JobConfig;
import lombok.Value;
import net.jqwik.api.*;
import net.jqwik.api.arbitraries.LongArbitrary;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;
import net.jqwik.api.stateful.ActionSequenceArbitrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

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

    @Property
    @Label("Given a valid sequence of actions, " +
            "and a max runs per second of 1 - 100, " +
            "when they are all run, " +
            "and a second passes between each action, " +
            "then they should be successful, " +
            "and it should never be at the limit when a second passes since the last run.")
    void validActions(
            @ForAll("actions") ActionSequence<LimiterWithClock> actions,
            @ForAll("reasonablyLowLimiterWithClock") LimiterWithClock limiter) {
        actions
                .withInvariant(l ->
                        assertThat(l.getLimiter().isAtLimit(), is(false)))
                .run(limiter);
    }

    @Provide
    ActionSequenceArbitrary<LimiterWithClock> actions() {
        return Arbitraries.sequences(Arbitraries.of(
                new CountRunsUnderPerSecondLimitAction(),
                new CountRunsOverPerSecondLimitAction()));
    }

    @Provide
    Arbitrary<LimiterWithClock> reasonablyLowLimiterWithClock() {
        LongArbitrary longArbitrary = Arbitraries.longs().between(1, 100);
        Long max = Long.MAX_VALUE;
        return longArbitrary
                .map(l -> new JobConfig.LimitConfig(l, max, max, max))
                .map(f -> {
                    ControllableClock clock = new ControllableClock(0);
                    return new LimiterWithClock(new RunJobRateLimit(f, clock), clock);
                });
    }

    @Provide
    Arbitrary<RunJobRateLimit> noLimit() {
        Long max = Long.MAX_VALUE;
        Arbitrary<JobConfig.LimitConfig> noLimitsConfig = Arbitraries.just(
                new JobConfig.LimitConfig(max, max, max, max));
        return noLimitsConfig.map(RunJobRateLimit::new);
    }

    static class CountRunsUnderPerSecondLimitAction implements Action<LimiterWithClock> {
        @Override
        public LimiterWithClock run(LimiterWithClock lwc) {
            RunJobRateLimit limiter = lwc.getLimiter();
            ControllableClock clock = lwc.getClock();
            Long maxRequestsPerSecond = limiter.getLimitConfig().getMaxGlobalRequestsPerSecond();
            for (int i = 0; i < maxRequestsPerSecond - 1; i++) {
                limiter.countRun();
            }
            boolean atLimit = limiter.isAtLimit();
            assertThat(atLimit, is(false));
            limiter.onJobComplete();
            clock.increment(TimeUnit.SECONDS.toNanos(1) + 1);
            return lwc;
        }
    }

    static class CountRunsOverPerSecondLimitAction implements Action<LimiterWithClock> {
        @Override
        public LimiterWithClock run(LimiterWithClock lwc) {
            RunJobRateLimit limiter = lwc.getLimiter();
            ControllableClock clock = lwc.getClock();
            Long maxRequestsPerSecond = limiter.getLimitConfig().getMaxGlobalRequestsPerSecond();
            for (int i = 0; i < maxRequestsPerSecond; i++) {
                limiter.countRun();
            }
            boolean atLimit = limiter.isAtLimit();
            assertThat(atLimit, is(true));
            limiter.onJobComplete();
            clock.increment(TimeUnit.SECONDS.toNanos(1) + 1);
            return lwc;
        }
    }

    @Value
    static class LimiterWithClock {
        RunJobRateLimit limiter;
        ControllableClock clock;
    }
}