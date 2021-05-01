package conjob.service;

import conjob.config.JobConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

class RunJobRateLimiterTest {
    @Property
    @Label("Given a number of concurrent runs, " +
            "and a max concurrent runs greater than or equal to it, " +
            "and no other limit restrictions, " +
            "when we test if we're at the limit, " +
            "then it should always be false.")
    void concurrentRunsLessThanMax(
            @ForAll @LongRange(max = 100) Long concurrentRunsCount,
            @ForAll @LongRange(max = 100) Long maxConcurrentRunsCount) {
        Assume.that(() -> concurrentRunsCount <= maxConcurrentRunsCount);
        JobConfig.LimitConfig limitConfig =
                new JobConfig.LimitConfig(Long.MAX_VALUE, maxConcurrentRunsCount, Long.MAX_VALUE, Long.MAX_VALUE);

        RunJobRateLimiter runJobRateLimiter = new RunJobRateLimiter(limitConfig);

        for (int i = 0; i < concurrentRunsCount; i++) {
            if (runJobRateLimiter.isLimitingOrIncrement())
                assertThat("Should not exceed limit", false);
        }
    }

    @Property
    @Label("Given a number of concurrent runs, " +
            "and a max concurrent runs less than it, " +
            "and no other limit restrictions, " +
            "when we test if we're at the limit, " +
            "then it should eventually be true.")
    void concurrentRunsGreaterThanMax(
            @ForAll @LongRange(max = 100) Long concurrentRunsCount,
            @ForAll @LongRange(max = 100) Long maxConcurrentRunsCount) {
        Assume.that(() -> concurrentRunsCount > maxConcurrentRunsCount);
        JobConfig.LimitConfig limitConfig =
                new JobConfig.LimitConfig(Long.MAX_VALUE, maxConcurrentRunsCount, Long.MAX_VALUE, Long.MAX_VALUE);

        RunJobRateLimiter runJobRateLimiter = new RunJobRateLimiter(limitConfig);

        boolean isAtLimit = false;
        for (int i = 0; i < concurrentRunsCount; i++) {
            isAtLimit = runJobRateLimiter.isLimitingOrIncrement();
            if (isAtLimit) break;
        }

        assertThat(isAtLimit, is(true));
    }

    @Property
    @Label("Given config with no limits, " +
            "when we test if we're at the limit, " +
            "then it should always be false.")
    void noLimits(
            @ForAll("actions") ActionSequence<RunJobRateLimiter> actions,
            @ForAll("noLimits") RunJobRateLimiter rateLimiter) {
        actions
                .withInvariant(limiter ->
                        assertThat(limiter.getRunningJobsCount(), is(greaterThanOrEqualTo(0))))
                .run(rateLimiter);
    }

    @Provide
    Arbitrary<ActionSequence<RunJobRateLimiter>> actions() {
        return Arbitraries.sequences(Arbitraries.of(
                new LimitedOrIncrementAction(), new DecrementAction()));
    }

    @Provide
    Arbitrary<RunJobRateLimiter> noLimits() {
        Long max = Long.MAX_VALUE;
        Arbitrary<JobConfig.LimitConfig> noLimitsConfig = Arbitraries.just(
                new JobConfig.LimitConfig(max, max, max, max));
        return noLimitsConfig.map(RunJobRateLimiter::new);
    }

    static class LimitedOrIncrementAction implements Action<RunJobRateLimiter> {
        @Override
        public boolean precondition(RunJobRateLimiter stack) {
            return stack.getRunningJobsCount() >= 0;
        }

        @Override
        public RunJobRateLimiter run(RunJobRateLimiter rateLimiter) {
            int originalCount = rateLimiter.getRunningJobsCount();
            boolean isLimiting = rateLimiter.isLimitingOrIncrement();
            int newCount = rateLimiter.getRunningJobsCount();
            assertThat(isLimiting, is(false));
            assertThat(newCount, is(originalCount + 1));
            return rateLimiter;
        }
    }

    static class DecrementAction implements Action<RunJobRateLimiter> {
        @Override
        public boolean precondition(RunJobRateLimiter rateLimiter) {
            return rateLimiter.getRunningJobsCount() > 0;
        }

        @Override
        public RunJobRateLimiter run(RunJobRateLimiter rateLimiter) {
            int originalCount = rateLimiter.getRunningJobsCount();
            rateLimiter.decrementRunningJobsCount();
            int newCount = rateLimiter.getRunningJobsCount();
            assertThat(newCount, is(originalCount - 1));
            return rateLimiter;
        }
    }
}