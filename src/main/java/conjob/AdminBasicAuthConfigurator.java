package conjob;

import conjob.config.AdminConfig;
import conjob.resource.auth.AdminConstraintSecurityHandler;
import io.dropwizard.setup.AdminEnvironment;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.util.security.Password;

import java.util.Objects;
import java.util.Optional;

public class AdminBasicAuthConfigurator {
    Optional<AbstractLoginService.UserPrincipal> configureAdminBasicAuth(AdminConfig adminConfig, AdminEnvironment adminEnv) {
        Optional<AbstractLoginService.UserPrincipal> userPrincipal = Optional.empty();
        if (credentialsAreSet(adminConfig)) {
            userPrincipal = Optional.of(enableBasicAuth(adminConfig, adminEnv));
        }
        return userPrincipal;
    }

    private boolean credentialsAreSet(AdminConfig config) {
        return Objects.nonNull(config.getUsername()) && Objects.nonNull(config.getPassword());
    }

    private AbstractLoginService.UserPrincipal enableBasicAuth(AdminConfig config, AdminEnvironment adminEnv) {
        AbstractLoginService.UserPrincipal userPrincipal =
                new AbstractLoginService.UserPrincipal(
                        config.getUsername(),
                        new Password(config.getPassword()));
        adminEnv.setSecurityHandler(
                new AdminConstraintSecurityHandler(
                        new AdminConstraintSecurityHandler.AdminLoginService(
                                userPrincipal)));
        return userPrincipal;
    }
}