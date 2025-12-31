package conjob.core.job;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectVolumeResponse;
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
    void listVolumesSuccess(@ForAll("mockVolumes") List<InspectVolumeResponse> givenMockVolumes)
             {
        List<InspectVolumeResponse> volumes = List.copyOf(givenMockVolumes);
        DockerClient client = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        DockerAdapter adapter = new DockerAdapter(client);

        when(client.listVolumesCmd().exec().getVolumes()).thenReturn(volumes);

        List<String> volumeNames = adapter.listAllVolumeNames();

        assertThat(volumeNames,
                containsInAnyOrder(givenMockVolumes.stream().map(InspectVolumeResponse::getName).toArray()));
    }

    @Provide
    ListArbitrary<InspectVolumeResponse> mockVolumes() {
        return Arbitraries.strings().map(string -> {
            InspectVolumeResponse volume = mock(InspectVolumeResponse.class, RETURNS_DEEP_STUBS);
            when(volume.getName()).thenReturn(string);
            return volume;
        }).list();
    }

    @Test
    @DisplayName("Given available volumes," +
            "when listing all volume names," +
            "and an unexpected Exception is thrown," +
            "should throw that exception")
    void readLogsUnexpectedException()  {
        DockerClient client = mock(DockerClient.class);
        DockerAdapter adapter = new DockerAdapter(client);
        when(client.listVolumesCmd()).thenThrow(new RuntimeException(""));

        assertThrows(RuntimeException.class, adapter::listAllVolumeNames);
    }
}