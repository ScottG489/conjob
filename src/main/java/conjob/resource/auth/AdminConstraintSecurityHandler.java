package conjob.resource.auth;

import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;

import java.util.Objects;

public class AdminConstraintSecurityHandler extends ConstraintSecurityHandler {

    private static final String ADMIN_ROLE = "admin";

    public AdminConstraintSecurityHandler(AdminLoginService adminLoginService) {
        final Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, ADMIN_ROLE);
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{ADMIN_ROLE});
        final ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");
        setAuthenticator(new BasicAuthenticator());
        addConstraintMapping(cm);
        setLoginService(adminLoginService);
    }

    public static class AdminLoginService extends AbstractLoginService {

        private final UserPrincipal adminPrincipal;
        private final String adminUserName;

        public AdminLoginService(UserPrincipal userPrincipal) {
            this.adminUserName = Objects.requireNonNull(userPrincipal.getName());
            this.adminPrincipal = userPrincipal;
        }

        @Override
        protected String[] loadRoleInfo(final UserPrincipal principal) {
            if (adminUserName.equals(principal.getName())) {
                return new String[]{ADMIN_ROLE};
            }
            return new String[0];
        }

        @Override
        protected UserPrincipal loadUserInfo(final String userName) {
            return adminUserName.equals(userName) ? adminPrincipal : null;
        }
    }
}

