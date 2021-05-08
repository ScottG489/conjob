package conjob.init;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockerClientCreatorTest {
    private DockerClientCreator dockerClientCreator;
    private DefaultDockerClient.Builder mockDockerClientBuilder;
    private AuthedDockerClientCreator mockAuthedDockerCreator;

    @BeforeTry
    public void beforeEach() {
        mockDockerClientBuilder = mock(DefaultDockerClient.Builder.class);
        mockAuthedDockerCreator = mock(AuthedDockerClientCreator.class);
        dockerClientCreator = new DockerClientCreator(mockDockerClientBuilder, mockAuthedDockerCreator);
    }

    @Property
    @Label("Given a null username or password, " +
            "and a docker client builder that returns a docker client, " +
            "when creating a docker client, " +
            "should should return the docker client that the builder returns.")
    void userOrPassNull(@ForAll("userOrPassNull") Tuple.Tuple2<String, String> userAndPass) throws DockerException, DockerCertificateException, InterruptedException {
        DefaultDockerClient mockDockerClient = mock(DefaultDockerClient.class);
        when(mockDockerClientBuilder.build()).thenReturn(mockDockerClient);

        DockerClient dockerClient = dockerClientCreator.createDockerClient(userAndPass.get1(), userAndPass.get2());

        assertThat(dockerClient, is(mockDockerClient));
    }

    @Property
    @Label("Given a username and password, " +
            "and an authed docker client builder that returns a docker client, " +
            "when creating a docker client, " +
            "should should return the docker client that the builder returns.")
    void userAndPassSupplied(@ForAll String username, @ForAll String password) throws DockerException, InterruptedException {
        DefaultDockerClient mockDockerClient = mock(DefaultDockerClient.class);
        when(mockAuthedDockerCreator.createDockerClient(username, password)).thenReturn(mockDockerClient);

        DockerClient dockerClient = dockerClientCreator.createDockerClient(username, password);

        assertThat(dockerClient, is(mockDockerClient));
    }

    @Provide
    Arbitrary<Tuple.Tuple2<String, String>> userOrPassNull() {
        return Arbitraries.strings().injectNull(.5).tuple2()
                .filter(t -> t.get1() == null || t.get2() == null);
    }

}