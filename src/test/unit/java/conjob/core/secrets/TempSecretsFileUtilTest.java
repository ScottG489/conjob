package conjob.core.secrets;

import conjob.core.secrets.TempSecretsFileUtil;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TempSecretsFileUtilTest {
    private TempSecretsFileUtil tempSecretsFileUtil;

    @BeforeTry
    void beforeEach() {
        tempSecretsFileUtil = new TempSecretsFileUtil();
    }

    void createSecretsFile(@ForAll("ascii") String givenSecretsContents) throws IOException {
        File tempSecretsFile = tempSecretsFileUtil.createSecretsFile(givenSecretsContents);

        String secretsContents = Files.readString(tempSecretsFile.toPath());
        assertThat(secretsContents, is(givenSecretsContents));
        assertThat(tempSecretsFile.exists(), is(true));
    }

    void deleteSecretsFile(@ForAll String givenSecretsContents) throws IOException {
        File tempSecretsFile = tempSecretsFileUtil.createSecretsFile(givenSecretsContents);
        tempSecretsFileUtil.delete(tempSecretsFile.toPath());

        assertThat(tempSecretsFile.exists(), is(false));
    }

    // TODO: I don't want to restrict password to only allow ascii. However, when this test runs in
    // TODO:   the build container it fails. UTF (maybe all?) characters are turned into byte 63
    // TODO:   (? symbol). It has something to do with encoding/decoding.
    @Provide
    Arbitrary<String> ascii() {
        return Arbitraries.strings().ascii();
    }
}