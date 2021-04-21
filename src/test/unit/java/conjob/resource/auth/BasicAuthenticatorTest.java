package conjob.resource.auth;

import io.dropwizard.auth.basic.BasicCredentials;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.UseType;
import org.eclipse.jetty.security.AbstractLoginService;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class BasicAuthenticatorTest {

    @Property
    @Label("Given basic credentials, " +
            "when it's authenticated against itself, " +
            "then the authentication should return a user principal.")
    void authenticateMatchingCreds(@ForAll @UseType BasicCredentials basicCreds) {
        Optional<AbstractLoginService.UserPrincipal> userPrincipal =
                new BasicAuthenticator(basicCreds.getUsername(), basicCreds.getPassword())
                        .authenticate(basicCreds);

        assertThat(userPrincipal.isPresent(), is(true));
    }

    @Property
    @Label("Given basic credentials, " +
            "and different credentials, " +
            "when the latter is authenticated against the former, " +
            "then the authentication should not return a user principal.")
    void authenticateIncorrectCreds(
            @ForAll @UseType BasicCredentials basicCreds,
            @ForAll @UseType BasicCredentials differentCreds) {
        Assume.that(!basicCreds.equals(differentCreds));

        Optional<AbstractLoginService.UserPrincipal> userPrincipal =
                new BasicAuthenticator(basicCreds.getUsername(), basicCreds.getPassword())
                        .authenticate(differentCreds);

        assertThat(userPrincipal.isPresent(), is(false));
    }
}