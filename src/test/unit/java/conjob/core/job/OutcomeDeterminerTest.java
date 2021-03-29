package conjob.core.job;

import conjob.core.job.exception.UnknownOutcomeStatusCodeException;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.JobRunOutcome;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutcomeDeterminerTest {
    private OutcomeDeterminer outcomeDeterminer;

    @BeforeTry
    void setUp() {
        outcomeDeterminer = new OutcomeDeterminer();
    }

    @Property
    @Label("Given an exit status code of 0, " +
            "when determining the conclusion, " +
            "then the conclusion should be that it was successful.")
    void determineOutcomeSuccess(@ForAll String output) {
        Long exitStatusCode = 0L;
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, output);

        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(jobRunOutcome);

        assertThat(jobRunConclusion, is(JobRunConclusion.SUCCESS));
    }

    @Property
    @Label("Given an exit status code of -1, " +
            "when determining the conclusion, " +
            "then the conclusion should be that it timed out.")
    void determineOutcomeTimedOut(@ForAll String output) {
        Long exitStatusCode = -1L;
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, output);

        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(jobRunOutcome);

        assertThat(jobRunConclusion, is(JobRunConclusion.TIMED_OUT));
    }

    @Property
    @Label("Given any positive exit status code, " +
            "when determining the conclusion, " +
            "then the conclusion should be that it failed.")
    void determineOutcomeFailure(
            @ForAll @Positive long exitStatusCode,
            @ForAll String output) {
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, output);

        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(jobRunOutcome);

        assertThat(jobRunConclusion, is(JobRunConclusion.FAILURE));
    }

    @Property
    @Label("Given any exit status code less than -1, " +
            "when determining the conclusion, " +
            "then the conclusion should be that it is an illegal status code.")
    void determineOutcomeUnknownOutcomeStatusCodeException(
            @ForAll @LongRange(min = Integer.MIN_VALUE, max = -2) long exitStatusCode,
            @ForAll String output) {
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, output);

        assertThrows(UnknownOutcomeStatusCodeException.class, () -> outcomeDeterminer.determineOutcome(jobRunOutcome));
    }
}