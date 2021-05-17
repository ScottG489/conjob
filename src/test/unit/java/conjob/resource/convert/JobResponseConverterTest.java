package conjob.resource.convert;

import conjob.api.JobRunConclusionResponse;
import conjob.api.JobRunResponse;
import conjob.core.job.model.JobRun;
import conjob.core.job.model.JobRunConclusion;
import net.jqwik.api.*;
import net.jqwik.api.arbitraries.LongArbitrary;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class JobResponseConverterTest {
    JobResponseConverter jobResponseConverter;

    @BeforeTry
    void beforeEach() {
        jobResponseConverter = new JobResponseConverter();
    }

    @Property
    @Label("Given a job run, " +
            "when converting it to a job run reponse, " +
            "should convert correctly.")
    void from(@ForAll("jobRun") JobRun jobRun) {
        JobRunResponse jobRunResponse = jobResponseConverter.from(jobRun);

        assertThat(jobRunResponse.getConclusion(), instanceOf(JobRunConclusionResponse.class));
        assertThat(jobRunResponse.getOutput(), is(jobRun.getOutput()));
        assertThat(jobRunResponse.getExitCode(), is(jobRun.getExitCode()));
        assertThat(jobRunResponse.getMessage(), is(not(emptyOrNullString())));
    }

    @Provide
    Arbitrary<JobRun> jobRun() {
        Arbitrary<JobRunConclusion> jrc = Arbitraries.of(JobRunConclusion.class).injectNull(.1);
        Arbitrary<String> of = Arbitraries.strings();
        LongArbitrary of1 = Arbitraries.longs();

        return Combinators.combine(jrc, of, of1).as(JobRun::new);
    }
}
