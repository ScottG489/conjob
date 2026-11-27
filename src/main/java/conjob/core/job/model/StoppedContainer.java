package conjob.core.job.model;

import lombok.Value;

import java.time.Instant;

// A job container that isn't running, and when it stopped.
@Value
public class StoppedContainer {
    String id;
    boolean removeImage;
    Instant stoppedAt;
}
