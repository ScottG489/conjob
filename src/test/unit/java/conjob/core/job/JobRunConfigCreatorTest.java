package conjob.core.job;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class JobRunConfigCreatorTest {

    @Test
    void determineOutcomeSuccess() {
        JobRunConfigCreator jobRunConfigCreator = new JobRunConfigCreator();
        jobRunConfigCreator.getContainerConfig(null, null, null);
//        assertThat();
    }

}