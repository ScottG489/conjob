package conjob.service;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
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

    @Property
    void createSecretsFile(@ForAll String givenSecretsContents) throws IOException {
        File tempSecretsFile = tempSecretsFileUtil.createSecretsFile(givenSecretsContents);

        String secretsContents = Files.readString(tempSecretsFile.toPath());
        assertThat(secretsContents, is(givenSecretsContents));
        assertThat(tempSecretsFile.exists(), is(true));
    }

    @Property
    void deleteSecretsFile(@ForAll String givenSecretsContents) throws IOException {
        File tempSecretsFile = tempSecretsFileUtil.createSecretsFile(givenSecretsContents);
        tempSecretsFileUtil.delete(tempSecretsFile.toPath());

        assertThat(tempSecretsFile.exists(), is(false));
    }
}