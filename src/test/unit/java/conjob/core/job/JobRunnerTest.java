package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.ReadLogsException;
import conjob.core.job.exception.RemoveImageException;
import conjob.core.job.exception.RunJobException;
import conjob.core.job.exception.StopJobRunException;
import conjob.core.job.model.JobRunOutcome;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.mockito.AdditionalAnswers;
import org.mockito.InOrder;
import testsupport.ExpectedLogs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;


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
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemoveImage) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given a container id, " +
            "and an infinite timeout, " +
            "when running the container, " +
            "and there is a problem waiting for it to finish, " +
            "and it returns an non-terminated exit code, " +
            "and it returns output, " +
            "should return an outcome with the same data.")
    void runContainerException(
            @ForAll String givenContainerId,
            @ForAll("nonTerminatedExitCodes") long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemoveImage) throws RunJobException, ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenThrow(new RunJobException(new Exception()));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @ExpectedLogs(JobRunner.class)
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
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemoveImage) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenAnswer(AdditionalAnswers.answersWithDelay(1000, (foo) -> null));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @ExpectedLogs(JobRunner.class)
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
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemoveImage) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenAnswer(AdditionalAnswers.answersWithDelay(1000, (foo) -> null));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(-1L));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @ExpectedLogs(JobRunner.class)
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
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemoveImage) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenAnswer(AdditionalAnswers.answersWithDelay(1000, (foo) -> null));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenThrow(new StopJobRunException(new Exception()));
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

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
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll boolean givenRemoveImage)
            throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenThrow(new ReadLogsException(new Exception()));

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(""));
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given a container id, " +
            "when running the container, " +
            "and there is a problem starting, " +
            "should return an outcome with exit code -1 and empty output.")
    void runContainerStartException(
            @ForAll String givenContainerId,
            @ForAll boolean givenRemoveImage) throws RunJobException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        doThrow(new RunJobException(new Exception()))
                .when(adapterMock).startContainer(givenContainerId);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(-1L));
        assertThat(jobRunOutcome.getOutput(), is(""));
    }

    @Property
    @Label("Given removeImage is true, " +
            "when running the container, " +
            "should call dockerAdapter.removeImage with the image ID resolved from the container.")
    void removeImageCalledWhenTrue(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll String givenImageId) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImageId(givenContainerId))
                .thenReturn(givenImageId);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true);

        verify(adapterMock).removeImage(givenImageId);
    }

    @Property
    @Label("Given removeImage is false, " +
            "when running the container, " +
            "should not call dockerAdapter.removeImage or dockerAdapter.getContainerImageId.")
    void removeImageNotCalledWhenFalse(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, false);

        verify(adapterMock, never()).removeImage(any());
        verify(adapterMock, never()).getContainerImageId(any());
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given removeImage is true, " +
            "when running the container, " +
            "and removing the image throws, " +
            "should still return the outcome.")
    void removeImageExceptionSwallowed(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll String givenImageId) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImageId(givenContainerId))
                .thenReturn(givenImageId);
        doThrow(new RemoveImageException(new Exception()))
                .when(adapterMock).removeImage(givenImageId);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @Label("Given removeImage is true, " +
            "when running the container, " +
            "should look up the image ID before starting the container, " +
            "and remove the image after.")
    void imageIdLookedUpBeforeStart(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll String givenImageId) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImageId(givenContainerId))
                .thenReturn(givenImageId);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true);

        InOrder inOrder = inOrder(adapterMock);
        inOrder.verify(adapterMock).getContainerImageId(givenContainerId);
        inOrder.verify(adapterMock).startContainer(givenContainerId);
        inOrder.verify(adapterMock).removeImage(givenImageId);
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given removeImage is true, " +
            "when running the container, " +
            "and looking up the image ID throws, " +
            "should not call removeImage, " +
            "and still return the outcome.")
    void imageIdLookupExceptionSwallowed(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImageId(givenContainerId))
                .thenThrow(new InspectContainerException(new Exception()));

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
        verify(adapterMock, never()).removeImage(any());
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
