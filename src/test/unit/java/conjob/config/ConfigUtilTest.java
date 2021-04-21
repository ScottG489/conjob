package conjob.config;

import conjob.core.job.config.ConfigUtil;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ConfigUtilTest {
    private ConfigUtil configUtil;

    @BeforeTry
    void beforeEach() {
        configUtil = new ConfigUtil();
    }

    @Property
    @Label("Given an image identifier, " +
            "when it's translated to a volume name, " +
            "then the volume name replaces the image name separators with dashes.")
    void imageIdentifierNameWithTag(
            @ForAll("imageIdentifierPart") String imageDomain,
            @ForAll("imageIdentifierPart") String imageName,
            @ForAll("imageIdentifierPart") String imageTag) {
        String volumeNameForImage = configUtil
                .translateToVolumeName(imageDomain + "/" + imageName + ":" + imageTag);

        assertThat(volumeNameForImage, is(imageDomain + "-" + imageName + "-" + imageTag));
    }

    @Property
    @Label("Given an image identifier, " +
            "and it doesn't have a tag, " +
            "when it's translated to a volume name, " +
            "then the volume name replaces the image name separators with dashes.")
    void imageIdentifierNameWithoutTag(
            @ForAll("imageIdentifierPart") String imageDomain,
            @ForAll("imageIdentifierPart") String imageName) {
        String volumeNameForImage = configUtil
                .translateToVolumeName(imageDomain + "/" + imageName);

        assertThat(volumeNameForImage, is(imageDomain + "-" + imageName));
    }

    @Provide
    Arbitrary<String> imageIdentifierPart() {
        return Arbitraries.strings().excludeChars('/', ':');
    }
}