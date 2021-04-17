package conjob.core.job;

import conjob.core.job.exception.ReadLogsException;
import conjob.core.job.exception.RunJobException;
import conjob.core.job.exception.StopJobRunException;
import conjob.core.job.model.JobRunOutcome;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobRunnerTest {
    @Property
    @Label("Given a container id, " +
            "and an infinite timeout, " +
            "when running the container, " +
            "and it returns an exit code, " +
            "and it returns output, " +
            "should return an outcome with the same data.")
    void runContainer(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.startContainerThenWaitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property(tries = 1)
    @Label("Given a container id, " +
            "and an infinite timeout, " +
            "when running the container, " +
            "and there is a problem running or waiting for it to finish, " +
            "and it returns an non-terminated exit code, " +
            "and it returns output, " +
            "should return an outcome with the same data.")
    void runContainerException(
            @ForAll String givenContainerId,
            @ForAll("nonTerminatedExitCodes") long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.startContainerThenWaitForExit(givenContainerId))
                .thenThrow(new RunJobException(new Exception()));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @Label("Given a container id, " +
            "and an immediate timeout, " +
            "when running the container, " +
            "and it's requested to be stopped, " +
            "and it returns an non-terminated exit code, " +
            "and it returns output, " +
            "should return an outcome with the same output, " +
            "and returns the exit code from stopping the container.")
    void runContainerTimeout(
            @ForAll String givenContainerId,
            @ForAll("nonTerminatedExitCodes") long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @Label("Given a container id, " +
            "and an immediate timeout, " +
            "when running the container, " +
            "and it's requested to be stopped, " +
            "and it returns a terminated exit code, " +
            "and it returns output, " +
            "should return an outcome with the same output, " +
            "and returns an exit code of -1.")
    void runContainerTimeoutTerminated(
            @ForAll String givenContainerId,
            @ForAll("terminatedExitCodes") long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout);

        assertThat(jobRunOutcome.getExitStatusCode(), is(-1L));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @Label("Given a container id, " +
            "and an immediate timeout, " +
            "when running the container, " +
            "and it's requested to be stopped, " +
            "and there is a problem stopping, " +
            "and it returns output, " +
            "should return an outcome with the same output, " +
            "and returns an exit code of -1.")
    void runContainerTimeoutTerminatedException(
            @ForAll String givenContainerId,
            @ForAll String givenContainerOutput) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenThrow(new StopJobRunException(new Exception()));
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout);

        assertThat(jobRunOutcome.getExitStatusCode(), is(-1L));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @Label("Given a container id, " +
            "and an infinite timeout, " +
            "when running the container, " +
            "and it returns an exit code, " +
            "and it returns output, " +
            "should return an outcome with the same data.")
    void runContainerReadLogsException(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode)
            throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.startContainerThenWaitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenThrow(new ReadLogsException(new Exception()));

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(""));
    }

    @Provide
    Arbitrary<Long> nonTerminatedExitCodes() {
        final long SIGKILL = 137;
        final long SIGTERM = 143;
        return Arbitraries.longs()
                .between(0, 255)
                .filter(l -> !l.equals(SIGKILL) && !l.equals(SIGTERM));
    }

    @Provide
    Arbitrary<Long> terminatedExitCodes() {
        final long SIGKILL = 137;
        final long SIGTERM = 143;
        return Arbitraries.of(SIGKILL, SIGTERM);
    }
}
