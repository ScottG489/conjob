package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Volume;
import com.spotify.docker.client.shaded.com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterListVolumesTest {
    @Test
    @DisplayName("Given no available volumes," +
            "when listing all volume names," +
            "should return an empty list")
    void listVolumesEmpty() throws DockerException, InterruptedException {
        DockerClient client = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        DockerAdapter adapter = new DockerAdapter(client);
        when(client.listVolumes().volumes()).thenReturn(ImmutableList.of());

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames, is(empty()));
    }

    @Test
    @DisplayName("Given available volumes," +
            "when listing all volume names," +
            "should list the names of the available volumes")
    void listVolumesSuccess() throws DockerException, InterruptedException {
        DockerClient client = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        DockerAdapter adapter = new DockerAdapter(client);

        Volume volume1 = mock(Volume.class);
        Volume volume2 = mock(Volume.class);
        ImmutableList<Volume> volumes = ImmutableList.of(volume1, volume2);
        when(client.listVolumes().volumes()).thenReturn(volumes);
        when(volume1.name()).thenReturn("volume_name_1");
        when(volume2.name()).thenReturn("volume_name_2");

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames, containsInAnyOrder("volume_name_1", "volume_name_2"));
    }

    @Test
    @DisplayName("Given available volumes," +
            "when listing all volume names," +
            "and an unexpected Exception is thrown," +
            "should throw that exception")
    void readLogsUnexpectedException() throws DockerException, InterruptedException {
        DockerClient client = mock(DockerClient.class);
        DockerAdapter adapter = new DockerAdapter(client);
        doThrow(new RuntimeException("")).when(client).listVolumes();

        assertThrows(RuntimeException.class, adapter::listAllVolumeNames);
    }
}