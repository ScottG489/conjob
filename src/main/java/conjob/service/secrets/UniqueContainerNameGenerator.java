package conjob.service.secrets;

import java.util.UUID;

public class UniqueContainerNameGenerator {
    String generate(String prefix) {
        return prefix + UUID.randomUUID();
    }
}