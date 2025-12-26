package conjob.init;

import conjob.config.AdminConfig;
import conjob.resource.auth.AdminConstraintSecurityHandler;
import io.dropwizard.core.setup.AdminEnvironment;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.security.Password;

import java.util.Objects;
import java.util.Optional;

public class AdminBasicAuthConfigurator {
    public Optional<UserPrincipal> configureAdminBasicAuth(AdminConfig adminConfig, AdminEnvironment adminEnv) {
        Optional<UserPrincipal> userPrincipal = Optional.empty();
        if (credentialsAreSet(adminConfig)) {
            userPrincipal = Optional.of(enableBasicAuth(adminConfig, adminEnv));
        }
        return userPrincipal;
    }

    private boolean credentialsAreSet(AdminConfig config) {
        return Objects.nonNull(config.getUsername()) && Objects.nonNull(config.getPassword());
    }

    private UserPrincipal enableBasicAuth(AdminConfig config, AdminEnvironment adminEnv) {
        UserPrincipal userPrincipal =
                new UserPrincipal(
                        config.getUsername(),
                        new Password(config.getPassword()));
        adminEnv.setSecurityHandler(
                new AdminConstraintSecurityHandler(
                        new AdminConstraintSecurityHandler.AdminLoginService(
                                userPrincipal)));
        return userPrincipal;
    }
}