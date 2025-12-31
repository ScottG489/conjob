package conjob.core.secrets;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import conjob.core.secrets.exception.CopySecretsToContainerException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SecretsDockerAdapterCopyToVolumeTest {

    private DockerClient mockClient;
    private SecretsDockerAdapter secretsAdapter;

    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class);
        secretsAdapter = new SecretsDockerAdapter(mockClient);
    }

    private CopyArchiveToContainerCmd setupCopyToVolumeMock(Path secretsFile, String containerId, String destinationPath) {
        CopyArchiveToContainerCmd mockCmd = mock(CopyArchiveToContainerCmd.class);
        when(mockClient.copyArchiveToContainerCmd(containerId)).thenReturn(mockCmd);
        when(mockCmd.withHostResource(secretsFile.toString())).thenReturn(mockCmd);
        when(mockCmd.withRemotePath(destinationPath)).thenReturn(mockCmd);
        return mockCmd;
    }

    @Property
    @Label("Given a secrets file, " +
            "and a container id, " +
            "and destination path, " +
            "when copying the secrets file, " +
            "should should finish successfully.")
    void copySecretsToVolume(
            @ForAll("secretsFile") Path givenSecretsFile,
            @ForAll String givenContainerId,
            @ForAll String destinationPath) {
        CopyArchiveToContainerCmd mockCmd = setupCopyToVolumeMock(givenSecretsFile, givenContainerId, destinationPath);

        secretsAdapter.copySecretsToVolume(givenSecretsFile, givenContainerId, destinationPath);

        verify(mockCmd).exec();
    }

    @Property
    @Label("Given a secrets file, " +
            "and a container id, " +
            "and destination path, " +
            "when copying the secrets file, " +
            "and an Exception is thrown, " +
            "should throw a CopySecretsToContainerException.")
    void catchDockerException(
            @ForAll("secretsFile") Path secretsFile,
            @ForAll String containerId,
            @ForAll String destinationPath) {
        CopyArchiveToContainerCmd mockCmd = setupCopyToVolumeMock(secretsFile, containerId, destinationPath);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(CopySecretsToContainerException.class, () ->
                secretsAdapter.copySecretsToVolume(secretsFile, containerId, destinationPath));
    }

    @Property
    @Label("Given a secrets file, " +
            "and a container id, " +
            "and destination path, " +
            "when copying the secrets file, " +
            "and an unexpected is thrown, " +
            "should throw that exception.")
    void catchUnexpectedException(
            @ForAll("secretsFile") Path secretsFile,
            @ForAll String containerId,
            @ForAll String destinationPath) {
        CopyArchiveToContainerCmd mockCmd = setupCopyToVolumeMock(secretsFile, containerId, destinationPath);
        doThrow(new RuntimeException("")).when(mockCmd).exec();

        assertThrows(CopySecretsToContainerException.class, () ->
                secretsAdapter.copySecretsToVolume(secretsFile, containerId, destinationPath));
    }

    @Provide
    Arbitrary<Path> secretsFile() {
        return Arbitraries.strings()
                .alpha().numeric()
                .map(Path::of);
    }
}
