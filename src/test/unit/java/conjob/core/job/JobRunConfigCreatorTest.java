package conjob.core.job;


import conjob.core.job.model.JobRunConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class JobRunConfigCreatorTest {
    private JobRunConfigCreator jobRunConfigCreator;

    @BeforeTry
    void beforeEach() {
        jobRunConfigCreator = new JobRunConfigCreator();
    }

    @Property
    @Label("Given an image name, " +
            "and not null or empty input, " +
            "and secrets volume name, " +
            "when getting the container config, " +
            "should have all the same input values.")
    void getContainerConfig(
            @ForAll String givenImageName,
            @ForAll @NotEmpty String givenInput,
            @ForAll String givenSecretsVolumeName) {
        JobRunConfig containerConfig =
                jobRunConfigCreator.getContainerConfig(givenImageName, givenInput, givenSecretsVolumeName);

        assertThat(containerConfig.getJobName(), is(givenImageName));
        assertThat(containerConfig.getInput(), is(givenInput));
        assertThat(containerConfig.getSecretsVolumeName(), is(givenSecretsVolumeName));
    }

    @Property
    @Label("Given an image name, " +
            "and a null or empty input, " +
            "and secrets volume name, " +
            "when getting the container config, " +
            "should have all the same input values.")
    void getContainerConfigNoInput(
            @ForAll String givenImageName,
            @ForAll("nullOrEmpty") String givenInput,
            @ForAll String givenSecretsVolumeName) {
        JobRunConfig containerConfig =
                jobRunConfigCreator.getContainerConfig(givenImageName, givenInput, givenSecretsVolumeName);

        assertThat(containerConfig.getJobName(), is(givenImageName));
        assertThat(containerConfig.getInput(), is(nullValue()));
        assertThat(containerConfig.getSecretsVolumeName(), is(givenSecretsVolumeName));
    }

    @Provide
    Arbitrary<String> nullOrEmpty() {
        return Arbitraries.just("").injectNull(.5);
    }
}
