package conjob.core.job;

public class ImageTagEnsurer {
    private static final String DEFAULT_TAG = "latest";

    public String hasTagOrLatest(String imageName) {
        return imageName.contains(":")
                ? imageName
                : imageName.concat(":" + DEFAULT_TAG);
    }
}
