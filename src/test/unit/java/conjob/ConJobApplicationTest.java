package conjob;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConJobApplicationTest {
    @Test
    @DisplayName("Application should be named ConJob")
    public void getName() {
        ConJobApplication app = new ConJobApplication();
        assertThat(app.getName(), is("ConJob"));
    }
}
