package conjob.resource;

import conjob.service.secrets.SecretsService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class SecretsResourceTest {
    private SecretsResource secretsResource;
    private SecretsService mockSecretsService;

    @BeforeTry
    void beforeEach() {
        mockSecretsService = mock(SecretsService.class);
        secretsResource = new SecretsResource(mockSecretsService);
    }

    @Property
    void handleTextPost(
            @ForAll String givenImageName,
            @ForAll String givenInput) throws IOException {
        Response response = secretsResource.handlePost(givenImageName, givenInput);

        assertThat(response.getStatusInfo(), is(Response.Status.OK));
        verify(mockSecretsService, times(1))
                .createsSecret(givenImageName, givenInput);
    }

    @Provide
    Arbitrary<Response> responseMock() {
        return Arbitraries.just(mock(Response.class));
    }
}