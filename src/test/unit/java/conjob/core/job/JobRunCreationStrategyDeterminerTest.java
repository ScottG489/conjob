package conjob.core.job;


import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.UseType;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JobRunCreationStrategyDeterminerTest {
    private DockerAdapter dockerAdapter;

    @BeforeTry
    void setUp() {
        dockerAdapter = mock(DockerAdapter.class);
    }

    @Property
    void determineStrategyAlwaysPull(@ForAll @UseType JobRunConfig givenJobRunConfig,
                                     @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        when(dockerAdapter.createJobRun(givenJobRunConfig)).thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.ALWAYS)
                .createJobRun(givenJobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(1)).pullImage(givenJobRunConfig.getJobName());
        verify(dockerAdapter, times(1)).createJobRun(givenJobRunConfig);
    }

    @Property
    void determineStrategyNeverPull(@ForAll @UseType JobRunConfig givenJobRunConfig,
                                    @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        when(dockerAdapter.createJobRun(givenJobRunConfig)).thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.NEVER)
                .createJobRun(givenJobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(0)).pullImage(givenJobRunConfig.getJobName());
        verify(dockerAdapter, times(1)).createJobRun(givenJobRunConfig);
    }

    @Property
    void determineStrategyNotAbsent(@ForAll @UseType JobRunConfig givenJobRunConfig,
                                    @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        when(dockerAdapter.createJobRun(givenJobRunConfig)).thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.ABSENT)
                .createJobRun(givenJobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(0)).pullImage(givenJobRunConfig.getJobName());
        verify(dockerAdapter, times(1)).createJobRun(givenJobRunConfig);
    }

    @Property
    void determineStrategyAbsentPull(@ForAll @UseType JobRunConfig givenJobRunConfig,
                                     @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        when(dockerAdapter.createJobRun(givenJobRunConfig))
                .thenThrow(CreateJobRunException.class)
                .thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.ABSENT)
                .createJobRun(givenJobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(1)).pullImage(givenJobRunConfig.getJobName());
        verify(dockerAdapter, times(2)).createJobRun(givenJobRunConfig);
    }

    @Property
    void determineStrategyCreateJobRunFailure(@ForAll PullStrategy pullStrategy,
                                              @ForAll @UseType JobRunConfig givenJobRunConfig)
            throws CreateJobRunException {
        when(dockerAdapter.createJobRun(givenJobRunConfig))
                .thenThrow(CreateJobRunException.class);

        assertThrows(CreateJobRunException.class, () ->
                new JobRunCreationStrategyDeterminer(dockerAdapter)
                        .determineStrategy(pullStrategy)
                        .createJobRun(givenJobRunConfig));
    }
}