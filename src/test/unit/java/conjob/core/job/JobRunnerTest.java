package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.ReadLogsException;
import conjob.core.job.exception.RemoveContainerException;
import conjob.core.job.exception.RunJobException;
import conjob.core.job.exception.StopJobRunException;
import conjob.core.job.model.ContainerImage;
import conjob.core.job.model.JobRunOutcome;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.UseType;
import org.mockito.AdditionalAnswers;
import org.mockito.InOrder;
import testsupport.ExpectedLogs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws RunJobException, ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenThrow(new RunJobException(new Exception()));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenAnswer(AdditionalAnswers.answersWithDelay(1000, (foo) -> null));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenAnswer(AdditionalAnswers.answersWithDelay(1000, (foo) -> null));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws ReadLogsException, StopJobRunException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = 0;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenAnswer(AdditionalAnswers.answersWithDelay(1000, (foo) -> null));
        when(adapterMock.stopContainer(givenContainerId, givenKillTimeout))
                .thenThrow(new StopJobRunException(new Exception()));
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage)
            throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenThrow(new ReadLogsException(new Exception()));

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

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
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) throws RunJobException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        long givenTimeoutSeconds = Long.MAX_VALUE;
        int givenKillTimeout = Integer.MAX_VALUE;
        doThrow(new RunJobException(new Exception()))
                .when(adapterMock).startContainer(givenContainerId);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, givenTimeoutSeconds, givenKillTimeout, givenRemove, givenRemoveImage);

        assertThat(jobRunOutcome.getExitStatusCode(), is(-1L));
        assertThat(jobRunOutcome.getOutput(), is(""));
    }

    @Property
    @Label("Given remove is true, " +
            "when running the container, " +
            "should remove the container after it exits.")
    void removeContainerAfterExit(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemoveImage) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), Runnable::run);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, givenRemoveImage);

        InOrder inOrder = inOrder(adapterMock);
        inOrder.verify(adapterMock).startContainer(givenContainerId);
        inOrder.verify(adapterMock).waitForExit(givenContainerId);
        inOrder.verify(adapterMock).removeContainer(givenContainerId);
    }

    @Property
    @Label("Given remove and removeImage are false, " +
            "when running the container, " +
            "should not queue a cleanup, " +
            "or remove the container or its image.")
    void noCleanUpWhenFalse(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        Executor executorMock = mock(Executor.class);
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, executorMock);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, false, false);

        verify(executorMock, never()).execute(any());
        verify(adapterMock, never()).removeContainer(any());
        verify(adapterMock, never()).getContainerImage(any());
        verify(imageRemoverMock, never()).remove(any());
    }

    @Property
    @Label("Given remove and removeImage are true, " +
            "when running the container, " +
            "should return the outcome before removing the container or its image, " +
            "and remove them once the cleanup executor runs.")
    void cleanUpAfterReturning(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll @UseType ContainerImage givenImage) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        List<Runnable> queuedCleanups = new ArrayList<>();
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, queuedCleanups::add);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImage(givenContainerId))
                .thenReturn(givenImage);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
        verify(adapterMock, never()).removeContainer(any());
        verify(imageRemoverMock, never()).remove(any());
        assertThat(queuedCleanups.size(), is(1));

        queuedCleanups.get(0).run();

        verify(adapterMock).removeContainer(givenContainerId);
        verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given remove and removeImage are true, " +
            "when running the container, " +
            "and the cleanup executor rejects the cleanup because it's shutting down, " +
            "should still return the outcome.")
    void cleanUpRejected(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        Executor shutDownExecutor = cleanup -> {
            throw new RejectedExecutionException();
        };
        JobRunner jobRunner = new JobRunner(adapterMock, mock(ImageRemover.class), shutDownExecutor);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given remove and removeImage are true, " +
            "when running the container, " +
            "and removing the container throws, " +
            "should still remove the image, " +
            "and still return the outcome.")
    void removeContainerExceptionSwallowed(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll @UseType ContainerImage givenImage) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, Runnable::run);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImage(givenContainerId))
                .thenReturn(givenImage);
        doThrow(new RemoveContainerException(new Exception()))
                .when(adapterMock).removeContainer(givenContainerId);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
        verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given remove and removeImage are true, " +
            "when running the container, " +
            "and there is a problem starting, " +
            "should still remove the container and its image.")
    void startExceptionStillCleansUp(
            @ForAll String givenContainerId,
            @ForAll @UseType ContainerImage givenImage) throws RunJobException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, Runnable::run);
        when(adapterMock.getContainerImage(givenContainerId))
                .thenReturn(givenImage);
        doThrow(new RunJobException(new Exception()))
                .when(adapterMock).startContainer(givenContainerId);

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(-1L));
        assertThat(jobRunOutcome.getOutput(), is(""));
        verify(adapterMock).removeContainer(givenContainerId);
        verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @Label("Given removeImage is false, " +
            "when running the container, " +
            "should not look up or remove its image.")
    void removeImageNotCalledWhenFalse(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll boolean givenRemove) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, Runnable::run);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, givenRemove, false);

        verify(imageRemoverMock, never()).remove(any());
        verify(adapterMock, never()).getContainerImage(any());
    }

    @Property
    @Label("Given remove and removeImage are true, " +
            "when running the container, " +
            "should look up its image after it exits, " +
            "then remove the container, " +
            "then remove the image.")
    void cleanUpInOrder(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput,
            @ForAll @UseType ContainerImage givenImage) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, Runnable::run);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImage(givenContainerId))
                .thenReturn(givenImage);

        jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        InOrder inOrder = inOrder(adapterMock, imageRemoverMock);
        inOrder.verify(adapterMock).waitForExit(givenContainerId);
        inOrder.verify(adapterMock).getContainerImage(givenContainerId);
        inOrder.verify(adapterMock).removeContainer(givenContainerId);
        inOrder.verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @ExpectedLogs(JobRunner.class)
    @Label("Given remove and removeImage are true, " +
            "when running the container, " +
            "and looking up its image throws, " +
            "should not remove an image, " +
            "but still remove the container, " +
            "and still return the outcome.")
    void imageLookupExceptionSwallowed(
            @ForAll String givenContainerId,
            @ForAll @LongRange(max = 255) long givenContainerExitCode,
            @ForAll String givenContainerOutput) throws RunJobException, ReadLogsException {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobRunner jobRunner = new JobRunner(adapterMock, imageRemoverMock, Runnable::run);
        when(adapterMock.waitForExit(givenContainerId))
                .thenReturn(givenContainerExitCode);
        when(adapterMock.readAllLogsUntilExit(givenContainerId))
                .thenReturn(givenContainerOutput);
        when(adapterMock.getContainerImage(givenContainerId))
                .thenThrow(new InspectContainerException(new Exception()));

        JobRunOutcome jobRunOutcome =
                jobRunner.runContainer(givenContainerId, Long.MAX_VALUE, Integer.MAX_VALUE, true, true);

        assertThat(jobRunOutcome.getExitStatusCode(), is(givenContainerExitCode));
        assertThat(jobRunOutcome.getOutput(), is(givenContainerOutput));
        verify(imageRemoverMock, never()).remove(any());
        verify(adapterMock).removeContainer(givenContainerId);
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
