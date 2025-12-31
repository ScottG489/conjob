package conjob.init;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientConfig;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;

class DockerClientCreatorTest {
    private DockerClientCreator dockerClientCreator;
    private DockerClientConfig mockDockerClientConfig;
    private AuthedDockerClientCreator mockAuthedDockerCreator;

    @BeforeTry
    public void beforeEach() {
        mockDockerClientConfig = mock(DockerClientConfig.class);
        mockAuthedDockerCreator = mock(AuthedDockerClientCreator.class);
        when(mockDockerClientConfig.getDockerHost()).thenReturn(java.net.URI.create("unix:///var/run/docker.sock"));
        dockerClientCreator = new DockerClientCreator(mockDockerClientConfig, mockAuthedDockerCreator);
    }

    @Property
    @Label("Given a null username or password, " +
            "when creating a docker client, " +
            "should not use the authed docker client creator.")
    void userOrPassNull(@ForAll("userOrPassNull") Tuple.Tuple2<String, String> userAndPass) {
        dockerClientCreator.createDockerClient(userAndPass.get1(), userAndPass.get2());

        verify(mockAuthedDockerCreator, never()).createDockerClient(any(), any());
    }

    @Property
    @Label("Given a null username or password, " +
            "when creating a docker client, " +
            "should use the docker client config to get the docker host.")
    void userOrPassNullUsesConfig(@ForAll("userOrPassNull") Tuple.Tuple2<String, String> userAndPass) {
        dockerClientCreator.createDockerClient(userAndPass.get1(), userAndPass.get2());

        verify(mockDockerClientConfig).getDockerHost();
    }

    @Property
    @Label("Given a username and password, " +
            "when creating a docker client, " +
            "should delegate to the authed docker client creator.")
    void userAndPassSupplied(@ForAll String username, @ForAll String password) {
        DockerClient mockDockerClient = mock(DockerClient.class);
        when(mockAuthedDockerCreator.createDockerClient(username, password)).thenReturn(mockDockerClient);

        DockerClient dockerClient = dockerClientCreator.createDockerClient(username, password);

        assertThat(dockerClient, is(mockDockerClient));
    }

    @Property
    @Label("Given a username and password, " +
            "when creating a docker client, " +
            "should not use the docker client config.")
    void userAndPassSuppliedDoesNotUseConfig(@ForAll String username, @ForAll String password) {
        DockerClient mockDockerClient = mock(DockerClient.class);
        when(mockAuthedDockerCreator.createDockerClient(username, password)).thenReturn(mockDockerClient);

        dockerClientCreator.createDockerClient(username, password);

        verify(mockDockerClientConfig, never()).getDockerHost();
    }

    @Provide
    Arbitrary<Tuple.Tuple2<String, String>> userOrPassNull() {
        return Arbitraries.strings().injectNull(.5).tuple2()
                .filter(t -> t.get1() == null || t.get2() == null);
    }

}
