package conjob;

import conjob.config.AdminConfig;
import conjob.init.AdminBasicAuthConfigurator;
import conjob.resource.auth.AdminConstraintSecurityHandler;
import io.dropwizard.setup.AdminEnvironment;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.util.security.Password;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class AdminBasicAuthConfiguratorTest {
    private AdminBasicAuthConfigurator adminBasicAuthConfigurator;

    @BeforeTry
    public void beforeEach() {
        adminBasicAuthConfigurator = new AdminBasicAuthConfigurator();
    }

    @Property
    @Label("Given a username and password, " +
            "and an admin environment, " +
            "when configuring admin basic auth, " +
            "should register an admin security handler for basic auth, " +
            "and it should successfully authenticate against the given credentials.")
    void userAndPassSupplied(@ForAll String username, @ForAll String password) {
        AdminConfig adminConfig = new AdminConfig(username, password);
        AdminEnvironment mockAdminEnv = mock(AdminEnvironment.class);

        Optional<AbstractLoginService.UserPrincipal> userPrincipal =
                adminBasicAuthConfigurator.configureAdminBasicAuth(adminConfig, mockAdminEnv);

        userPrincipal.ifPresent(up ->
                assertThat(up.authenticate(new Password(password)), is(true)));
        verify(mockAdminEnv, times(1))
                .setSecurityHandler(any(AdminConstraintSecurityHandler.class));
    }

    @Property
    @Label("Given a null username or password, " +
            "and an admin environment, " +
            "when configuring admin basic auth, " +
            "should not register an admin security handler for basic auth.")
    void userOrPassNull(@ForAll("userOrPassNull") AdminConfig givenAdminConfig) {
        AdminEnvironment mockAdminEnv = mock(AdminEnvironment.class);

        Optional<AbstractLoginService.UserPrincipal> userPrincipal =
                adminBasicAuthConfigurator.configureAdminBasicAuth(givenAdminConfig, mockAdminEnv);

        assertThat(userPrincipal.isEmpty(), is(true));
        verify(mockAdminEnv, times(0)).setSecurityHandler(any());
    }

    @Provide
    Arbitrary<AdminConfig> userOrPassNull() {
        return Arbitraries.strings().injectNull(.5).tuple2()
                .filter(t -> t.get1() == null || t.get2() == null)
                .map(f -> new AdminConfig(f.get1(), f.get2()));
    }
}