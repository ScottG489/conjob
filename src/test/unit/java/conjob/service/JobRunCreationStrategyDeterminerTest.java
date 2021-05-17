package conjob.service;


import conjob.core.job.DockerAdapter;
import conjob.core.job.JobRunCreationStrategyDeterminer;
import conjob.core.job.exception.CreateJobRunException;
import conjob.core.job.exception.JobUpdateException;
import conjob.core.job.model.JobRunConfig;
import conjob.core.job.PullStrategy;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
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
    void determineStrategyAlwaysPull(@ForAll String jobName,
                                     @ForAll String input,
                                     @ForAll String secretsVolumeName,
                                     @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretsVolumeName);
        when(dockerAdapter.createJobRun(jobRunConfig)).thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.ALWAYS)
                .createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(1)).pullImage(jobName);
        verify(dockerAdapter, times(1)).createJobRun(jobRunConfig);
    }

    @Property
    void determineStrategyNeverPull(@ForAll String jobName,
                                    @ForAll String input,
                                    @ForAll String secretsVolumeName,
                                    @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretsVolumeName);
        when(dockerAdapter.createJobRun(jobRunConfig)).thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.NEVER)
                .createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(0)).pullImage(jobName);
        verify(dockerAdapter, times(1)).createJobRun(jobRunConfig);
    }

    @Property
    void determineStrategyNotAbsent(@ForAll String jobName,
                                    @ForAll String input,
                                    @ForAll String secretsVolumeName,
                                    @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretsVolumeName);
        when(dockerAdapter.createJobRun(jobRunConfig)).thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.ABSENT)
                .createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(0)).pullImage(jobName);
        verify(dockerAdapter, times(1)).createJobRun(jobRunConfig);
    }

    @Property
    void determineStrategyAbsentPull(@ForAll String jobName,
                                     @ForAll String input,
                                     @ForAll String secretsVolumeName,
                                     @ForAll String givenJobRunId)
            throws CreateJobRunException, JobUpdateException {
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretsVolumeName);
        when(dockerAdapter.createJobRun(jobRunConfig))
                .thenThrow(CreateJobRunException.class)
                .thenReturn(givenJobRunId);

        String jobRunId = new JobRunCreationStrategyDeterminer(dockerAdapter)
                .determineStrategy(PullStrategy.ABSENT)
                .createJobRun(jobRunConfig);

        assertThat(jobRunId, is(givenJobRunId));
        verify(dockerAdapter, times(1)).pullImage(jobName);
        verify(dockerAdapter, times(2)).createJobRun(jobRunConfig);
    }

    @Property
    void determineStrategyCreateJobRunFailure(@ForAll PullStrategy pullStrategy,
                                              @ForAll String jobName,
                                              @ForAll String input,
                                              @ForAll String secretsVolumeName)
            throws CreateJobRunException {
        JobRunConfig jobRunConfig = new JobRunConfig(jobName, input, secretsVolumeName);
        when(dockerAdapter.createJobRun(jobRunConfig))
                .thenThrow(CreateJobRunException.class);

        assertThrows(CreateJobRunException.class, () ->
                new JobRunCreationStrategyDeterminer(dockerAdapter)
                        .determineStrategy(pullStrategy)
                        .createJobRun(jobRunConfig));
    }
}