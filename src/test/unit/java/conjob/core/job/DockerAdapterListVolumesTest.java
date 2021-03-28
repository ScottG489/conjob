package conjob.core.job;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Volume;
import com.spotify.docker.client.shaded.com.google.common.collect.ImmutableList;
import net.jqwik.api.*;
import net.jqwik.api.arbitraries.ListArbitrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DockerAdapterListVolumesTest {
    @Property
    @Label("Given available volumes," +
            "when listing all volume names," +
            "should list the names of the available volumes")
    void listVolumesSuccess(@ForAll("mockVolumes") List<Volume> givenMockVolumes)
            throws DockerException, InterruptedException {
        ImmutableList<Volume> volumes = ImmutableList.copyOf(givenMockVolumes);
        DockerClient client = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        DockerAdapter adapter = new DockerAdapter(client);

        when(client.listVolumes().volumes()).thenReturn(volumes);

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames,
                containsInAnyOrder(givenMockVolumes.stream().map(Volume::name).toArray()));
    }

    @Provide
    ListArbitrary<Volume> mockVolumes() {
        return Arbitraries.strings().map(string -> {
            Volume volume = mock(Volume.class);
            when(volume.name()).thenReturn(string);
            return volume;
        }).list();
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