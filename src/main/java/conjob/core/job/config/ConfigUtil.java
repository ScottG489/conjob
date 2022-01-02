package conjob.core.job.config;

public class ConfigUtil {
    public String translateToSecretsVolumeName(String imageName) {
        int usernameSeparatorIndex = imageName.indexOf('/');
        int tagSeparatorIndex = imageName.lastIndexOf(':');
        StringBuilder sb = new StringBuilder(imageName);
        sb.setCharAt(usernameSeparatorIndex, '-');
        if (tagSeparatorIndex != -1) {
            sb.setCharAt(tagSeparatorIndex, '-');
        }

        return sb.toString();
    }

    public String translateToDockerCacheVolumeName(String imageName) {
        int usernameSeparatorIndex = imageName.indexOf('/');
        int tagSeparatorIndex = imageName.lastIndexOf(':');
        StringBuilder sb = new StringBuilder(imageName);
        sb.setCharAt(usernameSeparatorIndex, '-');
        if (tagSeparatorIndex != -1) {
            sb.setCharAt(tagSeparatorIndex, '-');
        }

        return "conjob-docker-cache-" + sb;
    }
}
