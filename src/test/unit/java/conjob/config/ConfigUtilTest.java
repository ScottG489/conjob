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
            "when it's translated to a secrets volume name, " +
            "then the volume name replaces the image name separators with dashes.")
    void imageIdentifierNameWithTagTranslatedToSecretsVolumeName(
            @ForAll("imageIdentifierPart") String imageDomain,
            @ForAll("imageIdentifierPart") String imageName,
            @ForAll("imageIdentifierPart") String imageTag) {
        String volumeNameForImage = configUtil
                .translateToSecretsVolumeName(imageDomain + "/" + imageName + ":" + imageTag);

        assertThat(volumeNameForImage, is(imageDomain + "-" + imageName + "-" + imageTag));
    }

    @Property
    @Label("Given an image identifier, " +
            "and it doesn't have a tag, " +
            "when it's translated to a secrets volume name, " +
            "then the volume name replaces the image name separators with dashes.")
    void imageIdentifierNameWithoutTagTranslatedToSecretsVolumeName(
            @ForAll("imageIdentifierPart") String imageDomain,
            @ForAll("imageIdentifierPart") String imageName) {
        String volumeNameForImage = configUtil
                .translateToSecretsVolumeName(imageDomain + "/" + imageName);

        assertThat(volumeNameForImage, is(imageDomain + "-" + imageName));
    }

    @Property
    @Label("Given an image identifier, " +
            "when it's translated to a docker cache volume name, " +
            "then the volume name replaces the image name separators with dashes.")
    void imageIdentifierNameWithTagTranslatedToDockerCacheVolumeName(
            @ForAll("imageIdentifierPart") String imageDomain,
            @ForAll("imageIdentifierPart") String imageName,
            @ForAll("imageIdentifierPart") String imageTag) {
        String volumeNameForImage = configUtil
                .translateToDockerCacheVolumeName(imageDomain + "/" + imageName + ":" + imageTag);

        assertThat(volumeNameForImage, is("conjob-docker-cache-" + imageDomain + "-" + imageName + "-" + imageTag));
    }

    @Property
    @Label("Given an image identifier, " +
            "and it doesn't have a tag, " +
            "when it's translated to a docker cache volume name, " +
            "then the volume name replaces the image name separators with dashes.")
    void imageIdentifierNameWithoutTagTranslatedToDockerCacheVolumeName(
            @ForAll("imageIdentifierPart") String imageDomain,
            @ForAll("imageIdentifierPart") String imageName) {
        String volumeNameForImage = configUtil
                .translateToDockerCacheVolumeName(imageDomain + "/" + imageName);

        assertThat(volumeNameForImage, is("conjob-docker-cache-" + imageDomain + "-" + imageName));
    }

    @Provide
    Arbitrary<String> imageIdentifierPart() {
        return Arbitraries.strings().excludeChars('/', ':');
    }
}