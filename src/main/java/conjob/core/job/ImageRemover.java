package conjob.core.job;

import conjob.core.job.exception.ImageInUseException;
import conjob.core.job.exception.JobRunException;
import conjob.core.job.model.ContainerImage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

// Removes a finished job's image, unless something else still needs it: a container, or a tag
// other than the one the job ran.
@Slf4j
public class ImageRemover {
    private static final Duration LOCK_RETRY_INTERVAL = Duration.ofSeconds(1);

    private final DockerAdapter dockerAdapter;
    private final Lock removalLock;

    // removalLock is the write half of a read-write lock. Jobs hold the read half from pulling
    // their image until their container exists, so an image can't be removed in between, which
    // would make the create fail.
    public ImageRemover(DockerAdapter dockerAdapter, Lock removalLock) {
        this.dockerAdapter = dockerAdapter;
        this.removalLock = removalLock;
    }

    public void remove(ContainerImage image) {
        if (!lockOnceNoJobIsStarting()) {
            return;
        }
        try {
            removeUnlessNeeded(image);
        } catch (ImageInUseException e) {
            log.info("Image '{}' is still in use, so it wasn't removed: {}", image.getId(), e.getMessage());
        } catch (JobRunException e) {
            log.warn("Problem removing image '{}': {}", image.getId(), e.getMessage(), e);
        } finally {
            removalLock.unlock();
        }
    }

    // tryLock() doesn't wait in line for the lock, so jobs that start while this waits don't
    // have to wait for this removal too.
    private boolean lockOnceNoJobIsStarting() {
        while (!removalLock.tryLock()) {
            try {
                Thread.sleep(LOCK_RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting to remove an image");
                return false;
            }
        }
        return true;
    }

    private void removeUnlessNeeded(ContainerImage image) {
        Optional<List<String>> tags = dockerAdapter.findImageTags(image.getId());
        if (tags.isEmpty()) {
            return;
        }

        boolean hasJobTag = hasJobTag(image);
        int otherTagCount = tags.get().size() - (hasJobTag ? 1 : 0);
        if (otherTagCount > 0) {
            // Removing the image by ID would remove its other tags too. With other tags left,
            // removing the job's tag by name removes only that tag.
            if (hasJobTag) {
                dockerAdapter.removeImage(image.getName());
            }
            log.info("Image '{}' still has other tags, so it wasn't removed", image.getId());
            return;
        }
        dockerAdapter.removeImage(image.getId());
    }

    // Whether the name the job ran still tags this image. The tag may have moved to a newer image
    // since the job's container was created, and a name with a digest isn't a tag.
    private boolean hasJobTag(ContainerImage image) {
        return !image.getName().contains("@") && dockerAdapter.findImageId(image.getName())
                .filter(image.getId()::equals)
                .isPresent();
    }
}
