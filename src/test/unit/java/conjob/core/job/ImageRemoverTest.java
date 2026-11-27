package conjob.core.job;

import conjob.core.job.exception.ImageInUseException;
import conjob.core.job.exception.InspectImageException;
import conjob.core.job.exception.RemoveImageException;
import conjob.core.job.model.ContainerImage;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Size;
import testsupport.ExpectedLogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class ImageRemoverTest {
    @Property
    @Label("Given an image with no tags, " +
            "when removing it, " +
            "should remove it by ID.")
    void removeUntaggedImage(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(List.of()));

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        verify(adapterMock).removeImage(givenImageId);
        verify(adapterMock, never()).removeImage(givenImageName);
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @Label("Given an image whose only tag is the job's, " +
            "when removing it, " +
            "should remove it by ID, " +
            "while holding the lock.")
    void removeImageWithOnlyJobTag(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(List.of(givenImageName)));
        when(adapterMock.findImageId(givenImageName)).thenReturn(Optional.of(givenImageId));
        List<Boolean> lockedWhileRemoving = new ArrayList<>();
        doAnswer(invocation -> lockedWhileRemoving.add(lock.isWriteLockedByCurrentThread()))
                .when(adapterMock).removeImage(givenImageId);

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        assertThat(lockedWhileRemoving, is(List.of(true)));
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("Given an image with the job's tag and other tags, " +
            "when removing it, " +
            "should only remove the job's tag.")
    void removeOnlyJobTag(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName,
            @ForAll @Size(min = 1, max = 5) List<@From("tags") String> givenOtherTags) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        List<String> tags = new ArrayList<>(givenOtherTags);
        tags.add(givenImageName);
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(tags));
        when(adapterMock.findImageId(givenImageName)).thenReturn(Optional.of(givenImageId));

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        verify(adapterMock).removeImage(givenImageName);
        verify(adapterMock, never()).removeImage(givenImageId);
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("Given an image with tags, " +
            "and the job's tag now points at another image or nothing, " +
            "when removing it, " +
            "should remove nothing.")
    void keepImageWithOnlyOtherTags(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName,
            @ForAll @Size(min = 1, max = 5) List<@From("tags") String> givenOtherTags,
            @ForAll("otherImageIds") Optional<String> givenJobTagImageId) {
        Assume.that(!givenJobTagImageId.equals(Optional.of(givenImageId)));
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(givenOtherTags));
        when(adapterMock.findImageId(givenImageName)).thenReturn(givenJobTagImageId);

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        verify(adapterMock, never()).removeImage(any());
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("Given an image with a tag, " +
            "and the job ran it by digest, " +
            "when removing it, " +
            "should remove nothing, " +
            "since a digest isn't a tag.")
    void digestIsNotATag(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenTag,
            @ForAll("imageIds") String givenDigest) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        String givenImageName = givenTag + "@" + givenDigest;
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(List.of(givenTag)));
        when(adapterMock.findImageId(givenImageName)).thenReturn(Optional.of(givenImageId));

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        verify(adapterMock, never()).removeImage(any());
    }

    @Property
    @Label("Given an image that's already gone, " +
            "when removing it, " +
            "should remove nothing.")
    void skipMissingImage(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.empty());

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        verify(adapterMock, never()).removeImage(any());
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("Given an image that Docker won't remove because a container uses it, " +
            "when removing it, " +
            "should not throw, " +
            "and should release the lock.")
    void imageInUseSwallowed(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(List.of(givenImageName)));
        when(adapterMock.findImageId(givenImageName)).thenReturn(Optional.of(givenImageId));
        doThrow(new ImageInUseException(new Exception()))
                .when(adapterMock).removeImage(givenImageId);

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("Given removing the image throws, " +
            "when removing it, " +
            "should not throw, " +
            "and should release the lock.")
    void removeImageExceptionSwallowed(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(List.of()));
        doThrow(new RemoveImageException(new Exception()))
                .when(adapterMock).removeImage(givenImageId);

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("Given inspecting the image throws, " +
            "when removing it, " +
            "should not throw, " +
            "should remove nothing, " +
            "and should release the lock.")
    void inspectImageExceptionSwallowed(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags(givenImageId)).thenThrow(new InspectImageException(new Exception()));

        new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage(givenImageId, givenImageName));

        verify(adapterMock, never()).removeImage(any());
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @Label("Given an image found by name whose only tag is the job's, " +
            "when removing it by name, " +
            "should remove it by ID.")
    void removeByName(
            @ForAll("imageIds") String givenImageId,
            @ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageId(givenImageName)).thenReturn(Optional.of(givenImageId));
        when(adapterMock.findImageTags(givenImageId)).thenReturn(Optional.of(List.of(givenImageName)));

        new ImageRemover(adapterMock, lock.writeLock()).removeByName(givenImageName);

        verify(adapterMock).removeImage(givenImageId);
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @Label("Given no image with a name, " +
            "when removing it by name, " +
            "should remove nothing.")
    void removeByNameMissing(@ForAll("tags") String givenImageName) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageId(givenImageName)).thenReturn(Optional.empty());

        new ImageRemover(adapterMock, lock.writeLock()).removeByName(givenImageName);

        verify(adapterMock, never()).removeImage(any());
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Property
    @ExpectedLogs(ImageRemover.class)
    @Label("When removing dangling images, " +
            "should prune them while holding the lock.")
    void removeDangling(@ForAll @LongRange(min = 0) long givenBytesFreed) {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        List<Boolean> lockedWhilePruning = new ArrayList<>();
        when(adapterMock.pruneDanglingImages()).thenAnswer(invocation -> {
            lockedWhilePruning.add(lock.isWriteLockedByCurrentThread());
            return givenBytesFreed;
        });

        new ImageRemover(adapterMock, lock.writeLock()).removeDangling();

        assertThat(lockedWhilePruning, is(List.of(true)));
        assertThat(lock.isWriteLocked(), is(false));
    }

    @Example
    @ExpectedLogs(ImageRemover.class)
    @Label("Given pruning throws, " +
            "when removing dangling images, " +
            "should not throw, " +
            "and should release the lock.")
    void removeDanglingExceptionSwallowed() {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.pruneDanglingImages()).thenThrow(new RemoveImageException(new Exception()));

        new ImageRemover(adapterMock, lock.writeLock()).removeDangling();

        assertThat(lock.isWriteLocked(), is(false));
    }

    @Example
    @Label("Given a job is starting, " +
            "when removing an image, " +
            "should wait until the job has started, " +
            "without keeping other jobs from starting meanwhile.")
    void waitForStartingJobs() throws Exception {
        DockerAdapter adapterMock = mock(DockerAdapter.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        when(adapterMock.findImageTags("image-id")).thenReturn(Optional.of(List.of()));
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch firstJobStarting = new CountDownLatch(1);
        CountDownLatch firstJobStarted = new CountDownLatch(1);
        try {
            executor.submit(() -> {
                lock.readLock().lock();
                try {
                    firstJobStarting.countDown();
                    firstJobStarted.await();
                } finally {
                    lock.readLock().unlock();
                }
                return null;
            });
            firstJobStarting.await();

            Future<?> removal = executor.submit(() ->
                    new ImageRemover(adapterMock, lock.writeLock()).remove(new ContainerImage("image-id", "image:tag")));
            Thread.sleep(200);
            verify(adapterMock, never()).removeImage(any());

            // A job that waits in line for the lock, like lock() does, isn't kept waiting.
            Future<Boolean> secondJobStarted = executor.submit(() -> {
                boolean locked = lock.readLock().tryLock(1, TimeUnit.SECONDS);
                if (locked) lock.readLock().unlock();
                return locked;
            });
            assertThat(secondJobStarted.get(5, TimeUnit.SECONDS), is(true));
            verify(adapterMock, never()).removeImage(any());

            firstJobStarted.countDown();
            removal.get(5, TimeUnit.SECONDS);
            verify(adapterMock).removeImage("image-id");
        } finally {
            firstJobStarted.countDown();
            executor.shutdownNow();
        }
    }

    @Provide
    Arbitrary<String> imageIds() {
        return Arbitraries.strings().withCharRange('a', 'f').numeric().ofLength(12).map(hex -> "sha256:" + hex);
    }

    @Provide
    Arbitrary<String> tags() {
        Arbitrary<String> repositories = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20);
        Arbitrary<String> versions = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10);
        return Combinators.combine(repositories, versions).as((repository, version) -> repository + ":" + version);
    }

    @Provide
    Arbitrary<Optional<String>> otherImageIds() {
        return imageIds().optional();
    }
}
