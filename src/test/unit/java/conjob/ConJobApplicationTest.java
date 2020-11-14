package conjob;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ConJobApplicationTest {
    @Test
    public void getName() {
        ConJobApplication app = new ConJobApplication();
        assertThat(app.getName(), is("ConJob"));
    }
}
