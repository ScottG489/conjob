package conjob.core.job;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ImageTagEnsurerTest {
    private ImageTagEnsurer imageTagEnsurer;

    @BeforeTry
    void setUp() {
        imageTagEnsurer = new ImageTagEnsurer();
    }

    @Property
    @Label("Given an image name with a tag," +
            "when ensuring it has a tag" +
            "then the result should be the same image name")
    void ensureImageNameWithTag(@ForAll("stringWithColon") String givenImageName) {
        String actualImageName = imageTagEnsurer.hasTagOrLatest(givenImageName);

        assertThat(actualImageName, is(givenImageName));
    }

    @Property
    @Label("Given an image name without a tag," +
            "when ensuring it has a tag" +
            "then the image name should have ':latest' appended")
    void ensureImageNameWithOutTag(@ForAll("stringWithOutColon") String givenImageName) {
        String actualImageName = imageTagEnsurer.hasTagOrLatest(givenImageName);

        assertThat(actualImageName, is(givenImageName + ":latest"));
    }

    @Provide
    Arbitrary<String> stringWithColon() {
        return Arbitraries.strings().withChars(':').ofMinLength(1);
    }

    @Provide
    Arbitrary<String> stringWithOutColon() {
        return Arbitraries.strings().excludeChars(':');
    }
}
