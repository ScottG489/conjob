package conjob.init;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.auth.FixedRegistryAuthSupplier;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.apache.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthedDockerClientCreatorTest {
    private AuthedDockerClientCreator dockerClientCreator;
    private DefaultDockerClient.Builder mockDockerClientBuilder;
    private RegistryAuth.Builder mockRegAuthBuilder;

    @BeforeTry
    public void beforeEach() {
        mockDockerClientBuilder = mock(DefaultDockerClient.Builder.class, RETURNS_DEEP_STUBS);
        mockRegAuthBuilder = mock(RegistryAuth.Builder.class, RETURNS_DEEP_STUBS);
        dockerClientCreator = new AuthedDockerClientCreator(mockDockerClientBuilder, mockRegAuthBuilder);
    }

    @Property
    @Label("Given a username and password, " +
            "and a docker client that successfully auths, " +
            "when a docker client is created, " +
            "should validate credentials, " +
            "and should return the docker client that the builder returns.")
    void successfulAuth(@ForAll String username, @ForAll String password) throws DockerException, InterruptedException {
        RegistryAuth mockRegistryAuth = mock(RegistryAuth.class);
        DefaultDockerClient mockDockerClient = mock(DefaultDockerClient.class);
        when(mockDockerClient.auth(mockRegistryAuth)).thenReturn(HttpStatus.SC_OK);
        when(mockRegAuthBuilder.username(username).password(password).build()).thenReturn(mockRegistryAuth);
        when(mockDockerClientBuilder.registryAuthSupplier(
                any(FixedRegistryAuthSupplier.class))
                .build())
                .thenReturn(mockDockerClient);

        DefaultDockerClient dockerClient = dockerClientCreator.createDockerClient(username, password);

        assertThat(dockerClient, is(mockDockerClient));
    }

    @Property
    @Label("Given a username and password, " +
            "and a docker client that unsuccessfully auths, " +
            "when a docker client is created, " +
            "should throw an exception.")
    void unsuccessfulAuth(
            @ForAll String username,
            @ForAll String password,
            @ForAll("nonHttpOKStatusCodes") int httpStatus) throws DockerException, InterruptedException {
        Assume.that(httpStatus != 200);

        RegistryAuth mockRegistryAuth = mock(RegistryAuth.class);
        DefaultDockerClient mockDockerClient = mock(DefaultDockerClient.class);
        when(mockDockerClient.auth(mockRegistryAuth)).thenReturn(httpStatus);
        when(mockRegAuthBuilder.username(username).password(password).build()).thenReturn(mockRegistryAuth);
        when(mockDockerClientBuilder.registryAuthSupplier(
                any(FixedRegistryAuthSupplier.class))
                .build())
                .thenReturn(mockDockerClient);

        assertThrows(RuntimeException.class, () -> dockerClientCreator.createDockerClient(username, password));
    }

    @Provide
    Arbitrary<Integer> nonHttpOKStatusCodes() {
        return Arbitraries.integers().between(100, 507).filter(i -> i != HttpStatus.SC_OK);
    }
}