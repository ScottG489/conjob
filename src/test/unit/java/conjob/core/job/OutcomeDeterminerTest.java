package conjob.core.job;

import conjob.core.job.model.JobRunConclusion;
import conjob.core.job.model.JobRunOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OutcomeDeterminerTest {
    private OutcomeDeterminer outcomeDeterminer;

    @BeforeEach
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
}