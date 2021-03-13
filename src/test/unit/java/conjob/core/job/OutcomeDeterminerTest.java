package conjob.core.job;

import conjob.core.job.exception.UnknownOutcomeStatusCodeException;
import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.JobRunOutcome;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutcomeDeterminerTest {
    private OutcomeDeterminer outcomeDeterminer;

    @BeforeEach
    @BeforeTry
    void setUp() {
        outcomeDeterminer = new OutcomeDeterminer();
    }

    @Test
    void determineOutcomeSuccess() {
        Long exitStatusCode = 0L;
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, "");

        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(jobRunOutcome);

        assertThat(jobRunConclusion, is(JobRunConclusion.SUCCESS));
    }

    @Test
    void determineOutcomeTimedOut() {
        Long exitStatusCode = -1L;
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, "");

        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(jobRunOutcome);

        assertThat(jobRunConclusion, is(JobRunConclusion.TIMED_OUT));
    }

    @Property
    boolean determineOutcomeFailure(@ForAll @Positive long exitStatusCode) {
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, "");

        JobRunConclusion jobRunConclusion = outcomeDeterminer.determineOutcome(jobRunOutcome);

        assertThat(jobRunConclusion, is(JobRunConclusion.FAILURE));
        return true;
    }

    @Property
    boolean determineOutcomeUnknownOutcomeStatusCodeException(
            @ForAll @LongRange(min = Integer.MIN_VALUE, max = -2) long exitStatusCode) {
        JobRunOutcome jobRunOutcome = new JobRunOutcome(exitStatusCode, "");

        assertThrows(UnknownOutcomeStatusCodeException.class, () -> outcomeDeterminer.determineOutcome(jobRunOutcome));
        return true;
    }
}