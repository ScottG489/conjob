package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.AuthCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.MockedStatic;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthedDockerClientCreatorTest {
    private DockerClientConfig mockConfig;
    private AuthConfig mockAuthConfig;

    @BeforeTry
    public void beforeEach() {
        mockConfig = mock(DockerClientConfig.class);
        mockAuthConfig = mock(AuthConfig.class);
        when(mockConfig.getDockerHost()).thenReturn(URI.create("unix:///var/run/docker.sock"));
    }

    @Property
    @Label("Given a docker client that successfully auths, " +
            "when a docker client is created, " +
            "should validate credentials, " +
            "and should return the docker client.")
    void successfulAuth() {
        AuthedDockerClientCreator creator = new AuthedDockerClientCreator(mockConfig, mockAuthConfig);
        DockerClient mockDockerClient = mock(DockerClient.class);
        AuthCmd mockAuthCmd = mock(AuthCmd.class);
        AuthResponse mockAuthResponse = mock(AuthResponse.class);
        when(mockDockerClient.authCmd()).thenReturn(mockAuthCmd);
        when(mockAuthCmd.withAuthConfig(any())).thenReturn(mockAuthCmd);
        when(mockAuthCmd.exec()).thenReturn(mockAuthResponse);

        try (MockedStatic<DockerClientImpl> mockedStatic = mockStatic(DockerClientImpl.class)) {
            mockedStatic.when(() -> DockerClientImpl.getInstance(any(), any())).thenReturn(mockDockerClient);

            DockerClient dockerClient = creator.createDockerClient();

            assertThat(dockerClient, is(mockDockerClient));
            verify(mockAuthCmd).exec();
        }
    }

    @Property
    @Label("Given a docker client that unsuccessfully auths, " +
            "when a docker client is created, " +
            "should throw an exception.")
    void unsuccessfulAuth() {
        AuthedDockerClientCreator creator = new AuthedDockerClientCreator(mockConfig, mockAuthConfig);
        DockerClient mockDockerClient = mock(DockerClient.class);
        AuthCmd mockAuthCmd = mock(AuthCmd.class);
        when(mockDockerClient.authCmd()).thenReturn(mockAuthCmd);
        when(mockAuthCmd.withAuthConfig(any())).thenReturn(mockAuthCmd);
        when(mockAuthCmd.exec()).thenThrow(new RuntimeException("Auth failed"));

        try (MockedStatic<DockerClientImpl> mockedStatic = mockStatic(DockerClientImpl.class)) {
            mockedStatic.when(() -> DockerClientImpl.getInstance(any(), any())).thenReturn(mockDockerClient);

            assertThrows(RuntimeException.class, () -> creator.createDockerClient());
        }
    }
}
