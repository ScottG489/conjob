package dci;

import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

import java.util.Objects;

public class AdminConstraintSecurityHandler extends ConstraintSecurityHandler {

    private static final String ADMIN_ROLE = "admin";

    public AdminConstraintSecurityHandler(final String userName, final String password) {
        final Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, ADMIN_ROLE);
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{ADMIN_ROLE});
        final ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");
        setAuthenticator(new BasicAuthenticator());
        addConstraintMapping(cm);
        setLoginService(new AdminLoginService(userName, password));
    }

    public class AdminLoginService extends AbstractLoginService {

        private final UserPrincipal adminPrincipal;
        private final String adminUserName;

        public AdminLoginService(final String userName, final String password) {
            this.adminUserName = Objects.requireNonNull(userName);
            this.adminPrincipal = new UserPrincipal(userName, new Password(Objects.requireNonNull(password)));
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

