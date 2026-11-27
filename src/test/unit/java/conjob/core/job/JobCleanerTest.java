package conjob.core.job;

import conjob.core.job.exception.InspectContainerException;
import conjob.core.job.exception.ListContainersException;
import conjob.core.job.exception.RemoveContainerException;
import conjob.core.job.model.ContainerImage;
import conjob.core.job.model.StoppedContainer;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.UseType;
import org.mockito.InOrder;
import testsupport.ExpectedLogs;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class JobCleanerTest {
    private static final Instant QUEUED_AT = Instant.parse("2026-11-26T12:00:00Z");
    private static final long SETTLE_SECONDS = 60;

    @Property
    @Label("Given remove and removeImage are true, " +
            "when queuing a cleanup, " +
            "should not remove anything until the cleanup executor runs it, " +
            "then remove the container and its image.")
    void cleanUpLaterQueues(
            @ForAll String givenContainerId,
            @ForAll @UseType ContainerImage givenImage) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        List<Runnable> queued = new ArrayList<>();
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, queued::add, clockAt(QUEUED_AT));
        when(adapterMock.getContainerImage(givenContainerId)).thenReturn(givenImage);

        jobCleaner.cleanUpLater(givenContainerId, true, true);

        verifyNoInteractions(adapterMock, imageRemoverMock);
        assertThat(queued.size(), is(1));

        queued.get(0).run();

        verify(adapterMock).removeContainer(givenContainerId);
        verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @ExpectedLogs(JobCleaner.class)
    @Label("Given the cleanup executor rejects tasks because it's shutting down, " +
            "when queuing a cleanup, " +
            "should not throw.")
    void cleanUpRejected(
            @ForAll String givenContainerId,
            @ForAll String givenImageName,
            @ForAll boolean givenRemove,
            @ForAll boolean givenRemoveImage) {
        Executor shutDownExecutor = cleanup -> {
            throw new RejectedExecutionException();
        };
        JobCleaner jobCleaner = new JobCleaner(
                mock(DockerAdapter.class), mock(ImageRemover.class), shutDownExecutor, clockAt(QUEUED_AT));

        jobCleaner.cleanUpLater(givenContainerId, givenRemove, givenRemoveImage);
        jobCleaner.removeImageLater(givenImageName);
        jobCleaner.sweepLater();
    }

    @Property
    @Label("Given remove and removeImage are true, " +
            "when cleaning up, " +
            "should look up the container's image, " +
            "then remove the container, " +
            "then remove the image.")
    void cleanUpInOrder(
            @ForAll String givenContainerId,
            @ForAll @UseType ContainerImage givenImage) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, Runnable::run, clockAt(QUEUED_AT));
        when(adapterMock.getContainerImage(givenContainerId)).thenReturn(givenImage);

        jobCleaner.cleanUpLater(givenContainerId, true, true);

        InOrder inOrder = inOrder(adapterMock, imageRemoverMock);
        inOrder.verify(adapterMock).getContainerImage(givenContainerId);
        inOrder.verify(adapterMock).removeContainer(givenContainerId);
        inOrder.verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @Label("Given removeImage is false, " +
            "when cleaning up, " +
            "should remove the container only if remove is true, " +
            "and not look up or remove its image.")
    void removeImageFalse(
            @ForAll String givenContainerId,
            @ForAll boolean givenRemove) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, Runnable::run, clockAt(QUEUED_AT));

        jobCleaner.cleanUpLater(givenContainerId, givenRemove, false);

        verify(adapterMock, times(givenRemove ? 1 : 0)).removeContainer(givenContainerId);
        verify(adapterMock, never()).getContainerImage(any());
        verifyNoInteractions(imageRemoverMock);
    }

    @Property
    @ExpectedLogs(JobCleaner.class)
    @Label("Given remove and removeImage are true, " +
            "when cleaning up, " +
            "and removing the container throws, " +
            "should still remove the image.")
    void removeContainerExceptionSwallowed(
            @ForAll String givenContainerId,
            @ForAll @UseType ContainerImage givenImage) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, Runnable::run, clockAt(QUEUED_AT));
        when(adapterMock.getContainerImage(givenContainerId)).thenReturn(givenImage);
        doThrow(new RemoveContainerException(new Exception()))
                .when(adapterMock).removeContainer(givenContainerId);

        jobCleaner.cleanUpLater(givenContainerId, true, true);

        verify(imageRemoverMock).remove(givenImage);
    }

    @Property
    @ExpectedLogs(JobCleaner.class)
    @Label("Given remove and removeImage are true, " +
            "when cleaning up, " +
            "and looking up the container's image throws, " +
            "should still remove the container, " +
            "and not remove an image.")
    void imageLookupExceptionSwallowed(@ForAll String givenContainerId) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, Runnable::run, clockAt(QUEUED_AT));
        when(adapterMock.getContainerImage(givenContainerId))
                .thenThrow(new InspectContainerException(new Exception()));

        jobCleaner.cleanUpLater(givenContainerId, true, true);

        verify(adapterMock).removeContainer(givenContainerId);
        verifyNoInteractions(imageRemoverMock);
    }

    @Property
    @Label("Given an image name, " +
            "when queuing its removal, " +
            "should remove it by name once the cleanup executor runs it.")
    void removeImageLater(@ForAll String givenImageName) {
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        List<Runnable> queued = new ArrayList<>();
        JobCleaner jobCleaner = new JobCleaner(mock(DockerAdapter.class), imageRemoverMock, queued::add, clockAt(QUEUED_AT));

        jobCleaner.removeImageLater(givenImageName);

        verifyNoInteractions(imageRemoverMock);
        queued.forEach(Runnable::run);
        verify(imageRemoverMock).removeByName(givenImageName);
    }

    @Property
    @ExpectedLogs(JobCleaner.class)
    @Label("Given stopped containers to remove, " +
            "when sweeping, " +
            "should clean up the ones that stopped over a minute before the sweep was queued, " +
            "remove the images of the ones labeled to remove their image, " +
            "leave the rest, " +
            "and then remove dangling images.")
    void sweepSettledContainers(
            @ForAll("stoppedContainers") List<StoppedContainer> givenContainers) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        List<Runnable> queued = new ArrayList<>();
        SettableClock clock = new SettableClock(QUEUED_AT);
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, queued::add, clock);
        when(adapterMock.listStoppedContainersToRemove()).thenReturn(givenContainers);
        givenContainers.forEach(container -> when(adapterMock.getContainerImage(container.getId()))
                .thenReturn(imageOf(container)));

        jobCleaner.sweepLater();
        // The sweep waits in the queue, so it has to go by when it was queued, not when it runs.
        clock.set(QUEUED_AT.plusSeconds(3600));
        queued.forEach(Runnable::run);

        Instant settledBefore = QUEUED_AT.minusSeconds(SETTLE_SECONDS);
        for (StoppedContainer container : givenContainers) {
            boolean settled = container.getStoppedAt().isBefore(settledBefore);
            verify(adapterMock, times(settled ? 1 : 0)).removeContainer(container.getId());
            verify(imageRemoverMock, times(settled && container.isRemoveImage() ? 1 : 0)).remove(imageOf(container));
        }
        InOrder inOrder = inOrder(adapterMock, imageRemoverMock);
        inOrder.verify(adapterMock).listStoppedContainersToRemove();
        inOrder.verify(imageRemoverMock).removeDangling();
    }

    @Example
    @ExpectedLogs(JobCleaner.class)
    @Label("Given listing containers throws, " +
            "when sweeping, " +
            "should still remove dangling images.")
    void sweepListExceptionStillRemovesDanglingImages() {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ImageRemover imageRemoverMock = mock(ImageRemover.class);
        JobCleaner jobCleaner = new JobCleaner(adapterMock, imageRemoverMock, Runnable::run, clockAt(QUEUED_AT));
        when(adapterMock.listStoppedContainersToRemove()).thenThrow(new ListContainersException(new Exception()));

        jobCleaner.sweepLater();

        verify(adapterMock, never()).removeContainer(any());
        verify(imageRemoverMock).removeDangling();
    }

    @Provide
    Arbitrary<List<StoppedContainer>> stoppedContainers() {
        Arbitrary<String> ids = Arbitraries.strings().withCharRange('a', 'f').numeric().ofLength(12);
        Arbitrary<Long> secondsBeforeQueued = Arbitraries.longs().between(-600, 7200)
                .edgeCases(edgeCases -> edgeCases.add(SETTLE_SECONDS - 1, SETTLE_SECONDS, SETTLE_SECONDS + 1));
        return Combinators.combine(ids, Arbitraries.of(true, false), secondsBeforeQueued)
                .as((id, removeImage, seconds) -> new StoppedContainer(id, removeImage, QUEUED_AT.minusSeconds(seconds)))
                .list().ofMaxSize(8).uniqueElements(StoppedContainer::getId);
    }

    private static ContainerImage imageOf(StoppedContainer container) {
        return new ContainerImage("sha256:" + container.getId(), "image-of-" + container.getId() + ":latest");
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static class SettableClock extends Clock {
        private Instant now;

        SettableClock(Instant now) {
            this.now = now;
        }

        void set(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
