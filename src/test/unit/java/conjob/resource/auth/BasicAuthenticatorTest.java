package conjob.resource.auth;

import io.dropwizard.auth.basic.BasicCredentials;
import net.jqwik.api.*;
import net.jqwik.api.constraints.UseType;
import org.eclipse.jetty.security.AbstractLoginService;

import java.util.Optional;

import static net.jqwik.api.Arbitraries.oneOf;
import static net.jqwik.api.Arbitraries.strings;
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
        assertThat(userPrincipal.get().getName(), is(basicCreds.getUsername()));
    }

    @Property
    @Label("Given basic credentials, " +
            "and different credentials, " +
            "when the latter is authenticated against the former, " +
            "then the authentication should not return a user principal.")
    void authenticateIncorrectCreds(
            @ForAll("differentCreds") Tuple.Tuple2<BasicCredentials, BasicCredentials> differentCreds) {
        BasicCredentials basicCreds = differentCreds.get1();
        BasicCredentials otherCreds = differentCreds.get2();

        Optional<AbstractLoginService.UserPrincipal> userPrincipal =
                new BasicAuthenticator(basicCreds.getUsername(), basicCreds.getPassword())
                        .authenticate(otherCreds);

        assertThat(userPrincipal.isPresent(), is(false));
    }

    @Provide
    Arbitrary<Tuple.Tuple2<BasicCredentials, BasicCredentials>> differentCreds() {
        return oneOf(sameUserDiffPassCreds(), samePassDiffUsernameCreds(), allDifferentCreds());
    }

    private Arbitrary<Tuple.Tuple2<BasicCredentials, BasicCredentials>> sameUserDiffPassCreds() {
        return Combinators.combine(strings(), strings(), strings())
                .as((userA, passA, passB) ->
                        Tuple.of(new BasicCredentials(userA, passA), new BasicCredentials(userA, passB)))
                .filter(credPair -> !credPair.get1().equals(credPair.get2()));
    }

    private Arbitrary<Tuple.Tuple2<BasicCredentials, BasicCredentials>> samePassDiffUsernameCreds() {
        return Combinators.combine(strings(), strings(), strings())
                .as((userA, passA, userB) ->
                        Tuple.of(new BasicCredentials(userA, passA), new BasicCredentials(userB, passA)))
                .filter(credPair -> !credPair.get1().equals(credPair.get2()));
    }

    private Arbitrary<Tuple.Tuple2<BasicCredentials, BasicCredentials>> allDifferentCreds() {
        return Combinators.combine(strings(), strings(), strings(), strings())
                .as((userA, passA, userB, passB) ->
                        Tuple.of(new BasicCredentials(userA, passA), new BasicCredentials(userB, passA)))
                .filter(credPair -> !credPair.get1().equals(credPair.get2()));
    }
}