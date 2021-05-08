package conjob;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConJobApplicationTest {
    @Test
    @DisplayName("Given we start the server, " +
            "and the default production config is used, " +
            "when running the application, " +
            "then it should finish successfully.")
    public void startWithDefaultConfig() throws Exception {
        new ConJobApplication().run("server", "config.yml");
    }
}