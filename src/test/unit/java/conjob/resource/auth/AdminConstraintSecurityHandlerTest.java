package conjob.resource.auth;

import net.jqwik.api.*;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.RolePrincipal;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.security.Password;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class AdminConstraintSecurityHandlerTest {
    @Property
    @Label("Given an admin login service, " +
            "when the admin security handler is instantiated with it, " +
            "then it should be the login service for that security handler.")
    void adminConstraintSecurityHandler(
            @ForAll("adminLoginService") AdminConstraintSecurityHandler.AdminLoginService adminLoginService) {
        LoginService loginService =
                new AdminConstraintSecurityHandler(adminLoginService).getLoginService();

        assertThat(loginService, is(adminLoginService));
    }

    @Property
    @Label("Given a user principal, " +
            "and an admin login service instantiated with it, " +
            "when loading the role info for the same user principal, " +
            "then it should contain the role 'admin'.")
    void loadRoleInfoMatchingUserPrincipal(
            @ForAll("userPrincipal") UserPrincipal userPrincipal) {
        List<RolePrincipal> roleInfo = new AdminConstraintSecurityHandler.AdminLoginService(userPrincipal)
                .loadRoleInfo(userPrincipal);

        assertThat(roleInfo.size(), is(1));
        assertThat(roleInfo.get(0).getName(), is("admin"));
    }

    @Property
    @Label("Given a user principal, " +
            "when loading the role info for it, " +
            "then it should contain the role 'admin'.")
    void loadRoleInfoDifferentUserPrincipal(
            @ForAll("userPrincipal") UserPrincipal userPrincipal,
            @ForAll("userPrincipal") UserPrincipal differentUserPrincipal) {
        Assume.that(!userPrincipal.getName().equals(differentUserPrincipal.getName()));

        List<RolePrincipal> roleInfo = new AdminConstraintSecurityHandler.AdminLoginService(userPrincipal)
                .loadRoleInfo(differentUserPrincipal);

        assertThat(roleInfo, is(empty()));
    }

    @Property
    @Label("Given a user principal, " +
            "and an admin login service instantiated with it, " +
            "when loading the user info for the given user principal's name, " +
            "then it should be the same user principal.")
    void loadUserInfoMatchingUsername(
            @ForAll("userPrincipal") UserPrincipal userPrincipal) {
        UserPrincipal userInfo =
                new AdminConstraintSecurityHandler.AdminLoginService(userPrincipal)
                        .loadUserInfo(userPrincipal.getName());

        assertThat(userInfo, is(userPrincipal));
    }

    @Property
    @Label("Given a user principal, " +
            "and an admin login service instantiated with it, " +
            "when loading the user info for a different user principal's name, " +
            "then it should be the same user principal.")
    void loadUserInfoDifferentUsername(
            @ForAll("userPrincipal") UserPrincipal userPrincipal,
            @ForAll("userPrincipal") UserPrincipal differentUserPrincipal) {
        Assume.that(!userPrincipal.getName().equals(differentUserPrincipal.getName()));
        UserPrincipal userInfo =
                new AdminConstraintSecurityHandler.AdminLoginService(userPrincipal)
                        .loadUserInfo(differentUserPrincipal.getName());

        assertThat(userInfo, is(nullValue()));
    }

    @Provide
    Arbitrary<UserPrincipal> userPrincipal() {
        return Arbitraries.strings()
                .flatMap((username) -> Arbitraries.strings()
                        .map((password) -> new UserPrincipal(
                                username, new Password(password))));
    }

    @Provide
    Arbitrary<AdminConstraintSecurityHandler.AdminLoginService> adminLoginService() {
        return Arbitraries.strings()
                .flatMap((username) -> Arbitraries.strings()
                        .map((password) -> new UserPrincipal(
                                username, new Password(password))))
                .map(AdminConstraintSecurityHandler.AdminLoginService::new);
    }
}
