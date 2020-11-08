package dci.core.job.config;

public class ConfigUtil {
    public String translateToVolumeName(String imageName) {
        int usernameSeparatorIndex = imageName.indexOf('/');
        int tagSeparatorIndex = imageName.lastIndexOf(':');
        StringBuilder sb = new StringBuilder(imageName);
        sb.setCharAt(usernameSeparatorIndex, '-');
        if (tagSeparatorIndex != -1) {
            sb.setCharAt(tagSeparatorIndex, '-');
        }

        return sb.toString();
    }
}
