package conjob.core.secrets;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.secrets.exception.CopySecretsToContainerException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SecretsDockerAdapterCopyToVolumeTest {

    private DockerClient mockClient;
    private SecretsDockerAdapter secretsAdapter;

    @BeforeTry
    void setUp() {
        mockClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        secretsAdapter = new SecretsDockerAdapter(mockClient);
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
            @ForAll String destinationPath) throws DockerException, IOException, InterruptedException {
        secretsAdapter.copySecretsToVolume(givenSecretsFile, givenContainerId, destinationPath);

        verify(mockClient, times(1))
                .copyToContainer(givenSecretsFile, givenContainerId, destinationPath);
    }

    @Property
    @Label("Given a secrets file, " +
            "and a container id, " +
            "and destination path, " +
            "when copying the secrets file, " +
            "and a DockerException is thrown, " +
            "should throw a UpdateSecretsImageException.")
    void catchDockerException(
            @ForAll("secretsFile") Path secretsFile,
            @ForAll String containerId,
            @ForAll String destinationPath) throws DockerException, InterruptedException, IOException {
        doThrow(DockerException.class)
                .when(mockClient).copyToContainer(secretsFile, containerId, destinationPath);

        assertThrows(CopySecretsToContainerException.class, () ->
                secretsAdapter.copySecretsToVolume(secretsFile, containerId, destinationPath));
    }

    @Property
    @Label("Given a secrets file, " +
            "and a container id, " +
            "and destination path, " +
            "when copying the secrets file, " +
            "and a InterruptedException is thrown, " +
            "should throw a UpdateSecretsImageException.")
    void catchInterruptedException(
            @ForAll("secretsFile") Path secretsFile,
            @ForAll String containerId,
            @ForAll String destinationPath) throws DockerException, InterruptedException, IOException {
        doThrow(InterruptedException.class)
                .when(mockClient).copyToContainer(secretsFile, containerId, destinationPath);

        assertThrows(CopySecretsToContainerException.class, () ->
                secretsAdapter.copySecretsToVolume(secretsFile, containerId, destinationPath));
    }

    @Property
    @Label("Given a secrets file, " +
            "and a container id, " +
            "and destination path, " +
            "when copying the secrets file, " +
            "and a IOException is thrown, " +
            "should throw a UpdateSecretsImageException.")
    void catchIOException(
            @ForAll("secretsFile") Path secretsFile,
            @ForAll String containerId,
            @ForAll String destinationPath) throws DockerException, InterruptedException, IOException {
        doThrow(InterruptedException.class)
                .when(mockClient).copyToContainer(secretsFile, containerId, destinationPath);

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
            @ForAll String destinationPath) throws DockerException, InterruptedException, IOException {
        doThrow(InterruptedException.class)
                .when(mockClient).copyToContainer(secretsFile, containerId, destinationPath);

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