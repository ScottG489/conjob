package conjob.resource.admin.task;

import conjob.core.job.DockerAdapter;
import conjob.core.job.exception.RemoveVolumeException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.Size;
import org.mockito.stubbing.Answer;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class DockerVolumeRemoveTaskTest {

    @Property
    @Label("Given volume IDs, " +
            "when executing volume removal task, " +
            "then they should all be removed")
    void removeAllVolumeIds(@ForAll String postBody,
                            @ForAll @NotEmpty Map<
                                    @From("onlyIdKey") String,
                                    @Size(max = 10) List<@NotBlank String>> map) {
        DockerAdapter mockDockerAdapter = mock(DockerAdapter.class);
        PrintWriter writerMock = mock(PrintWriter.class);

        new DockerVolumeRemoveTask(mockDockerAdapter).execute(map, postBody, writerMock);

        map.get("id").forEach(value -> verify(mockDockerAdapter, times(1)).removeVolume(value));
    }

    @Property
    @Label("Given volume IDs, " +
            "and container IDs associated with those volumes, " +
            "and an error message containing the container IDs, " +
            "when executing volume removal task, " +
            "and removing them the first time fails, " +
            "then the volumes should be removed, " +
            "and the containers should be removed.")
    void removeAssociatedContainersAndVolumes(@ForAll String postBody,
                 @ForAll @NotEmpty Map<
                         @From("onlyIdKey") String,
                         @Size(max = 10) List<@NotBlank String>> map,
                 @ForAll("containerId") String associatedContainer1,
                 @ForAll("containerId") String associatedContainer2
    ) {
        Assume.that(!associatedContainer1.equals(associatedContainer2));

        String volumeRemoveErrorMessage = "Request error: DELETE unix://localhost:80/volumes/conjob-docker-cache-scottg489-docker-test-support-job-latest: 409, body: {\"message\":\"remove conjob-docker-cache-scottg489-docker-test-support-job-latest: volume is in use - [" + associatedContainer1 + ", " + associatedContainer2 + "]\"}\n";
        DockerAdapter mockDockerAdapter = mock(DockerAdapter.class);
        PrintWriter writerMock = mock(PrintWriter.class);

        RuntimeException innerCause = new RuntimeException(volumeRemoveErrorMessage);
        RuntimeException cause = new RuntimeException(innerCause);
        RemoveVolumeException mockException = new RemoveVolumeException(cause);

        doThrow(mockException).doAnswer((Answer<Void>) invocationOnMock -> null).when(mockDockerAdapter).removeVolume(any());

        new DockerVolumeRemoveTask(mockDockerAdapter).execute(map, postBody, writerMock);

        for (String volumeId: map.get("id")) {
            verify(mockDockerAdapter, times(2)).removeVolume(volumeId);
            verify(mockDockerAdapter, times(1)).removeContainer(associatedContainer1);
            verify(mockDockerAdapter, times(1)).removeContainer(associatedContainer2);
        }
    }

    @Provide
    Arbitrary<String> onlyIdKey() {
        return Arbitraries.of("id");
    }

    @Provide
    Arbitrary<String> containerId() {
        return Arbitraries.strings().withCharRange('a', 'f').ofLength(64);
    }
}
