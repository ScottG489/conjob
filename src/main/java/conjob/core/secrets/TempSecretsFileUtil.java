package conjob.core.secrets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class TempSecretsFileUtil {
    public File createSecretsFile(String secrets) throws IOException {
        File tempSecretsFile;
        String secretsTempDirPrefix = "secrets-temp-dir-";
        String secretsFileName = "secrets";

        File tempDirectory = getTempSecretsDir(secretsTempDirPrefix);
        tempSecretsFile = getTempSecretsFile(secretsFileName, tempDirectory);
        Files.write(tempSecretsFile.toPath(), secrets.getBytes());

        return tempSecretsFile;
    }

    public void delete(Path tempSecretsDirPath) throws IOException {
        Files.walk(tempSecretsDirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private File getTempSecretsFile(String secretsFileName, File tempDirectory) throws IOException {
        File tempFile = Files.createFile(new File(tempDirectory, secretsFileName).toPath()).toFile();
        tempFile.deleteOnExit();
        return tempFile;
    }

    private File getTempSecretsDir(String secretsTempDirNamePrefix) throws IOException {
        File tempDir = Files.createTempDirectory(secretsTempDirNamePrefix).toFile();
        tempDir.deleteOnExit();
        return tempDir;
    }
}