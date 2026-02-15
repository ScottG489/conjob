package conjob.init;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

class DockerClientCreatorFactoryTest {
    private DockerClientCreatorFactory factory;
    private DefaultDockerClientConfig.Builder mockConfigBuilder;

    @BeforeTry
    public void beforeEach() {
        mockConfigBuilder = mock(DefaultDockerClientConfig.Builder.class);
        when(mockConfigBuilder.build()).thenReturn(mock(DefaultDockerClientConfig.class));
        factory = new DockerClientCreatorFactory(mockConfigBuilder);
    }

    @Property
    @Label("Given a null username or password, " +
            "when creating a docker client creator, " +
            "should return a DefaultDockerClientCreator.")
    void nullCredentials_returnsDefaultCreator(
            @ForAll("userOrPassNull") Tuple.Tuple2<String, String> userAndPass) {
        DockerClientCreator result = factory.create(userAndPass.get1(), userAndPass.get2());

        assertThat(result, is(instanceOf(DefaultDockerClientCreator.class)));
    }

    @Property
    @Label("Given a username and password, " +
            "when creating a docker client creator, " +
            "should return an AuthedDockerClientCreator, " +
            "and should pass the username and password to the config builder.")
    void bothCredentialsProvided_returnsAuthedCreator(
            @ForAll String username, @ForAll String password) {
        when(mockConfigBuilder.withRegistryUsername(username)).thenReturn(mockConfigBuilder);
        when(mockConfigBuilder.withRegistryPassword(password)).thenReturn(mockConfigBuilder);

        DockerClientCreator result = factory.create(username, password);

        assertThat(result, is(instanceOf(AuthedDockerClientCreator.class)));
        verify(mockConfigBuilder).withRegistryUsername(username);
        verify(mockConfigBuilder).withRegistryPassword(password);
    }

    @Provide
    Arbitrary<Tuple.Tuple2<String, String>> userOrPassNull() {
        return Arbitraries.strings().injectNull(.5).tuple2()
                .filter(t -> t.get1() == null || t.get2() == null);
    }
}
