package conjob;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.util.security.Password;

import java.util.Objects;
import java.util.Optional;

class BasicAuthenticator implements Authenticator<BasicCredentials, AbstractLoginService.UserPrincipal> {
    private final String username;
    private final String password;

    BasicAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<AbstractLoginService.UserPrincipal> authenticate(BasicCredentials credentials) {
        if (username.equals(credentials.getUsername()) && password.equals(credentials.getPassword())) {
            return Optional.of(new AbstractLoginService.UserPrincipal(
                    credentials.getUsername(),
                    new Password(Objects.requireNonNull(password))));
        }
        return Optional.empty();
    }
}
