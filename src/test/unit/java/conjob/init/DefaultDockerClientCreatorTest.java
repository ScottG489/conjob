package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.MockedStatic;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

class DefaultDockerClientCreatorTest {
    private DefaultDockerClientCreator creator;
    private DockerClientConfig mockConfig;

    @BeforeTry
    public void beforeEach() {
        mockConfig = mock(DockerClientConfig.class);
        when(mockConfig.getDockerHost()).thenReturn(URI.create("unix:///var/run/docker.sock"));
        creator = new DefaultDockerClientCreator(mockConfig);
    }

    @Property
    @Label("When creating a docker client, " +
            "should return the docker client produced by DockerClientImpl " +
            "using the injected config and a non-null http client.")
    void returnsDockerClientFromGetInstance() {
        DockerClient mockDockerClient = mock(DockerClient.class);

        try (MockedStatic<DockerClientImpl> mockedStatic = mockStatic(DockerClientImpl.class)) {
            mockedStatic.when(() -> DockerClientImpl.getInstance(eq(mockConfig), isA(DockerHttpClient.class)))
                    .thenReturn(mockDockerClient);

            DockerClient result = creator.createDockerClient();

            assertThat(result, is(mockDockerClient));
        }
    }

    @Property
    @Label("When creating a docker client, " +
            "should read the docker host from the injected config.")
    void readsDockerHostFromConfig() {
        try (MockedStatic<DockerClientImpl> mockedStatic = mockStatic(DockerClientImpl.class)) {
            mockedStatic.when(() -> DockerClientImpl.getInstance(eq(mockConfig), isA(DockerHttpClient.class)))
                    .thenReturn(mock(DockerClient.class));

            creator.createDockerClient();

            verify(mockConfig).getDockerHost();
        }
    }
}
