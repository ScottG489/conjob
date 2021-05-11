package conjob.init;

import conjob.config.AuthConfig;
import conjob.resource.auth.BasicAuthenticator;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.eclipse.jetty.util.security.Password;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class BasicAuthConfiguratorTest {
    private BasicAuthConfigurator basicAuthConfigurator;

    @BeforeTry
    public void beforeEach() {
        basicAuthConfigurator = new BasicAuthConfigurator();
    }

    @Property
    @Label("Given a username and password, " +
            "and jersey environment, " +
            "when configuring basic auth, " +
            "should register a basic authenticator, " +
            "and it should successfully authenticate against the given credentials.")
    void userAndPassSupplied(@ForAll String username, @ForAll String password) {
        AuthConfig givenAuthConfig = new AuthConfig(username, password);
        JerseyEnvironment mockJerseyEnv = mock(JerseyEnvironment.class);

        Optional<BasicAuthenticator> basicAuthenticator =
                basicAuthConfigurator.configureBasicAuth(givenAuthConfig, mockJerseyEnv);

        basicAuthenticator.ifPresent(authenticator ->
                assertThat(authenticate(authenticator, username, password), is(true)));
        verify(mockJerseyEnv, times(1)).register(any(AuthDynamicFeature.class));
        verify(mockJerseyEnv, times(1)).register(RolesAllowedDynamicFeature.class);
    }

    @Property
    @Label("Given a null username or password, " +
            "and jersey environment, " +
            "when configuring basic auth, " +
            "should not register a basic authenticator.")
    void userOrPassNull(@ForAll("userOrPassNull") AuthConfig givenAuthConfig) {
        JerseyEnvironment mockJerseyEnv = mock(JerseyEnvironment.class);

        Optional<BasicAuthenticator> basicAuthenticator =
                basicAuthConfigurator.configureBasicAuth(givenAuthConfig, mockJerseyEnv);

        assertThat(basicAuthenticator.isEmpty(), is(true));
        verify(mockJerseyEnv, times(0)).register(any());
    }

    private Boolean authenticate(BasicAuthenticator auth, String username, String password) {
        BasicCredentials basicCredentials = new BasicCredentials(username, password);
        Password pass = new Password(password);
        return auth.authenticate(basicCredentials).map(p -> p.authenticate(pass)).orElse(false);
    }

    @Provide
    Arbitrary<AuthConfig> userOrPassNull() {
        return Arbitraries.strings().injectNull(.5).tuple2()
                .filter(t -> t.get1() == null || t.get2() == null)
                .map(f -> new AuthConfig(f.get1(), f.get2()));
    }
}