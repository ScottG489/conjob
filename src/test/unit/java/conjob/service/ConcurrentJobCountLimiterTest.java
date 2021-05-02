package conjob.service;

import conjob.config.JobConfig;
import net.jqwik.api.*;
import net.jqwik.api.arbitraries.LongArbitrary;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;
import net.jqwik.api.stateful.ActionSequenceArbitrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

class ConcurrentJobCountLimiterTest {

    @Property
    @Label("Given job count limiter, " +
            "and it has no limits, " +
            "when checking if it's at the limit, " +
            "then it should always be false.")
    void noLimit(@ForAll("noLimit") ConcurrentJobCountLimiter jobCountLimiter) {
        assertThat(jobCountLimiter.isAtLimit(), is(false));
    }

    @Property
    @Label("Given job count limiter, " +
            "and it has no limits, " +
            "when checking if it's at the limit, " +
            "then it should always be false.")
    void foo(
            @ForAll("noLimit") ConcurrentJobCountLimiter jobCountLimiter
    ) {
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
                new JobConfig.LimitConfig(max, 0L, max, max);

        boolean atLimit = new ConcurrentJobCountLimiter(noRunsAllowedConfig).isAtLimit();

        assertThat(atLimit, is(true));
    }


    @Property
    @Label("Given , " +
            "and , " +
            "when , " +
            "then .")
    void f(
            @ForAll("actions") ActionSequence<ConcurrentJobCountLimiter> actions,
            @ForAll("smallLimiter") ConcurrentJobCountLimiter limiter) {
        actions
                .withInvariant((ConcurrentJobCountLimiter l) ->
                        assertThat(
                                l.getCurrentlyRunningJobsCount(),
                                greaterThanOrEqualTo(0)))
                .run(limiter);
    }

    @Provide
    ActionSequenceArbitrary<ConcurrentJobCountLimiter> actions() {
        return Arbitraries.sequences(Arbitraries.of(
                new CountRunAction(),
                new JobCompleteAction(),
                new JobCompleteWhenNoneAction(),
                new CountRunOverMaxAction()));
    }

    @Provide
    Arbitrary<ConcurrentJobCountLimiter> smallLimiter() {
        LongArbitrary longArbitrary = Arbitraries.longs().between(0, 100);
        return longArbitrary
                .map(l -> new JobConfig.LimitConfig(l, l, l, l))
                .map(ConcurrentJobCountLimiter::new);
    }

    @Provide
    Arbitrary<ConcurrentJobCountLimiter> noLimit() {
        Long max = Long.MAX_VALUE;
        Arbitrary<JobConfig.LimitConfig> noLimitsConfig = Arbitraries.just(
                new JobConfig.LimitConfig(max, max, max, max));
        return noLimitsConfig.map(ConcurrentJobCountLimiter::new);
    }

    static class CountRunAction implements Action<ConcurrentJobCountLimiter> {
        @Override
        public ConcurrentJobCountLimiter run(ConcurrentJobCountLimiter limiter) {
            int originalCount = limiter.getCurrentlyRunningJobsCount();
            limiter.countRun();
            int currentCount = limiter.getCurrentlyRunningJobsCount();
            assertThat(currentCount, is(originalCount + 1));
            return limiter;
        }
    }

    static class CountRunOverMaxAction implements Action<ConcurrentJobCountLimiter> {
        @Override
        public ConcurrentJobCountLimiter run(ConcurrentJobCountLimiter limiter) {
            int originalCount = limiter.getCurrentlyRunningJobsCount();
            Long maxConcurrentRuns = limiter.getLimitConfig().getMaxConcurrentRuns();
            for (int i = 0; i < maxConcurrentRuns; i++) {
                limiter.countRun();
            }
            boolean atLimit = limiter.isAtLimit();
            long currentCount = limiter.getCurrentlyRunningJobsCount();
            assertThat(currentCount, is(originalCount + maxConcurrentRuns));
            assertThat(atLimit, is(true));
            return limiter;
        }
    }

    static class JobCompleteAction implements Action<ConcurrentJobCountLimiter> {
        @Override
        public boolean precondition(ConcurrentJobCountLimiter limiter) {
            return limiter.getCurrentlyRunningJobsCount() > 0;
        }

        @Override
        public ConcurrentJobCountLimiter run(ConcurrentJobCountLimiter limiter) {
            int originalCount = limiter.getCurrentlyRunningJobsCount();
            limiter.onJobComplete();
            int currentCount = limiter.getCurrentlyRunningJobsCount();
            assertThat(currentCount, is(originalCount - 1));
            return limiter;
        }
    }

    static class JobCompleteWhenNoneAction implements Action<ConcurrentJobCountLimiter> {
        @Override
        public boolean precondition(ConcurrentJobCountLimiter limiter) {
            return limiter.getCurrentlyRunningJobsCount() == 0;
        }

        @Override
        public ConcurrentJobCountLimiter run(ConcurrentJobCountLimiter limiter) {
            int originalCount = limiter.getCurrentlyRunningJobsCount();
            limiter.onJobComplete();
            int currentCount = limiter.getCurrentlyRunningJobsCount();
            assertThat(currentCount, is(originalCount));
            return limiter;
        }
    }
}