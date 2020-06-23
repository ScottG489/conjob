package dci.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigUtil {
    private static final Properties properties = new Properties();

    static {
        loadConfig();
    }

    public static String getFromConfig(String key) {
        return properties.get(key).toString();
    }

    public static void loadConfig() {
        try {
            File configProperties = new File("src/test/acceptance/resource/config.properties");
            FileInputStream fis = new FileInputStream(configProperties);
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
