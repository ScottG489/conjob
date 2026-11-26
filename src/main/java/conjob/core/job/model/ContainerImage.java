package conjob.core.job.model;

import lombok.Value;

// The image a container was created from: its ID, and the name the container was created with.
@Value
public class ContainerImage {
    String id;
    String name;
}
