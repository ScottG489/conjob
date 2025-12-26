package conjob.init;

import conjob.config.AuthConfig;
import conjob.resource.auth.BasicAuthenticator;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.eclipse.jetty.security.UserPrincipal;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.Objects;
import java.util.Optional;

public class BasicAuthConfigurator {
    public Optional<BasicAuthenticator> configureBasicAuth(AuthConfig authConfig, JerseyEnvironment jerseyEnv) {
        Optional<BasicAuthenticator> basicAuthenticator = Optional.empty();
        if (credentialsAreSet(authConfig)) {
            basicAuthenticator = Optional.of(enableBasicAuth(authConfig, jerseyEnv));
        }
        return basicAuthenticator;
    }

    private boolean credentialsAreSet(AuthConfig config) {
        return Objects.nonNull(config.getUsername()) && Objects.nonNull(config.getPassword());
    }

    private BasicAuthenticator enableBasicAuth(AuthConfig config, JerseyEnvironment jerseyEnv) {
        BasicAuthenticator auth = new BasicAuthenticator(
                config.getUsername(),
                config.getPassword());
        jerseyEnv.register(new AuthDynamicFeature(
                new BasicCredentialAuthFilter.Builder<UserPrincipal>()
                        .setAuthenticator(auth).buildAuthFilter()));
        jerseyEnv.register(RolesAllowedDynamicFeature.class);
        return auth;
    }
}