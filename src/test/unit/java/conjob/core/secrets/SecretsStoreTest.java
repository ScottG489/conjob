package conjob.core.secrets;

import com.spotify.docker.client.exceptions.DockerException;
import conjob.core.job.DockerAdapter;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecretsStoreTest {
    private DockerAdapter mockAdapter;
    private SecretsStore secretsStore;

    @BeforeTry
    void setUp() {
        mockAdapter = mock(DockerAdapter.class);
        secretsStore = new SecretsStore(mockAdapter);
    }

    @Property
    @Label("Given a secrets volume name, " +
            "and a list of volumes that contains it, " +
            "when finding a secrets volume for the given name, " +
            "then the secrets volume name should exist, " +
            "and it should have the same name as the requested one.")
    void secretsVolumeNameExists(
            @ForAll String secretsVolumeName,
            @ForAll List<String> secretVolumeNames)
            throws SecretsStoreException, DockerException, InterruptedException {
        secretVolumeNames.add(secretsVolumeName);
        Collections.shuffle(secretVolumeNames);
        when(mockAdapter.listAllVolumeNames()).thenReturn(secretVolumeNames);

        Optional<String> secrets = secretsStore.findSecrets(secretsVolumeName);

        assertThat(secrets.isPresent(), is(true));
        assertThat(secrets.get(), is(secretsVolumeName));
    }

    @Property
    @Label("Given a secrets volume name, " +
            "and a list of volumes that doesn't contain it, " +
            "when finding a secrets volume for the given name, " +
            "then the secrets volume name should not exist.")
    void secretsVolumeNameDoesNotExist(
            @ForAll String secretsVolumeName,
            @ForAll List<String> secretVolumeNames)
            throws SecretsStoreException, DockerException, InterruptedException {
        Assume.that(!secretVolumeNames.contains(secretsVolumeName));
        when(mockAdapter.listAllVolumeNames()).thenReturn(secretVolumeNames);

        Optional<String> secrets = secretsStore.findSecrets(secretsVolumeName);

        assertThat(secrets.isPresent(), is(false));
    }

    @Property
    @Label("Given a secrets volume name, " +
            "when finding a secrets volume for the given name, " +
            "and a DockerException is thrown, " +
            "then should throw a SecretsStoreException.")
    void findSecretsDockerException(
            @ForAll String secretsVolumeName)
            throws DockerException, InterruptedException {
        when(mockAdapter.listAllVolumeNames()).thenThrow(DockerException.class);

        assertThrows(
                SecretsStoreException.class,
                () -> secretsStore.findSecrets(secretsVolumeName));
    }

    @Property
    @Label("Given a secrets volume name, " +
            "when finding a secrets volume for the given name, " +
            "and a InterruptedException is thrown, " +
            "then should throw a SecretsStoreException.")
    void findSecretsInterruptedException(
            @ForAll String secretsVolumeName)
            throws DockerException, InterruptedException {
        when(mockAdapter.listAllVolumeNames()).thenThrow(InterruptedException.class);

        assertThrows(
                SecretsStoreException.class,
                () -> secretsStore.findSecrets(secretsVolumeName));
    }

    @Property
    @Label("Given a secrets volume name, " +
            "when finding a secrets volume for the given name, " +
            "and an unexpected exception is thrown, " +
            "then should throw that exception.")
    void findSecretsUnexpectedException(
            @ForAll String secretsVolumeName)
            throws DockerException, InterruptedException {
        when(mockAdapter.listAllVolumeNames()).thenThrow(RuntimeException.class);

        assertThrows(
                RuntimeException.class,
                () -> secretsStore.findSecrets(secretsVolumeName));
    }
}