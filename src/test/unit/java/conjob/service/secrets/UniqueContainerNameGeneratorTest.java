package conjob.service.secrets;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

class UniqueContainerNameGeneratorTest {
    private static final String UUID_REGEX = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";

    @Property
    void getIntermediaryContainerName(@ForAll String containerNamePrefix) {
        String uniqueContainerName =
                new UniqueContainerNameGenerator().generate(containerNamePrefix);

        assertThat(
                uniqueContainerName,
                matchesPattern(Pattern.quote(containerNamePrefix) + UUID_REGEX));
    }
}