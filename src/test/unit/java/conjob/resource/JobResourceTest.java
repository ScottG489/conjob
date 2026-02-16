package conjob.resource;

import conjob.api.JobRunResponse;
import conjob.core.job.model.JobRun;
import conjob.core.secrets.SecretsStoreException;
import conjob.resource.convert.JobResponseConverter;
import conjob.resource.convert.ResponseCreator;
import conjob.service.job.JobService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.UseType;
import net.jqwik.api.lifecycle.BeforeTry;

import jakarta.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobResourceTest {
    private JobResource jobResource;
    private JobService jobServiceMock;
    private ResponseCreator responseCreatorMock;
    private JobResponseConverter responseConverterMock;

    @BeforeTry
    void beforeEach() {
        jobServiceMock = mock(JobService.class);
        responseCreatorMock = mock(ResponseCreator.class);
        responseConverterMock = mock(JobResponseConverter.class);
        jobResource = new JobResource(jobServiceMock, responseCreatorMock, responseConverterMock);
    }

    @Property
    void handleTextPost(
            @ForAll String givenImageName,
            @ForAll String givenInput,
            @ForAll String givenPullStrategy,
            @ForAll boolean givenUseDockerCache,
            @ForAll @UseType JobRun jobRun,
            @ForAll @UseType JobRunResponse jobRunResponse,
            @ForAll("responseMock") Response givenMockResponse) throws SecretsStoreException {
        when(jobServiceMock.runJob(givenImageName, givenInput, givenPullStrategy, givenUseDockerCache))
                .thenReturn(jobRun);
        when(responseConverterMock.from(jobRun))
                .thenReturn(jobRunResponse);
        when(responseCreatorMock.createResponseFrom(jobRunResponse))
                .thenReturn(givenMockResponse);

        Response response = jobResource.handleTextPost(givenImageName, givenInput, givenPullStrategy, givenUseDockerCache);

        assertThat(response, is(givenMockResponse));
    }

    @Property
    void handleJsonPost(
            @ForAll String givenImageName,
            @ForAll String givenInput,
            @ForAll String givenPullStrategy,
            @ForAll boolean givenUseDockerCache,
            @ForAll @UseType JobRun jobRun,
            @ForAll @UseType JobRunResponse jobRunResponse,
            @ForAll("responseMock") Response givenMockResponse) throws SecretsStoreException {
        when(jobServiceMock.runJob(givenImageName, givenInput, givenPullStrategy, givenUseDockerCache))
                .thenReturn(jobRun);
        when(responseConverterMock.from(jobRun))
                .thenReturn(jobRunResponse);
        when(responseCreatorMock.createJsonResponseFrom(jobRunResponse))
                .thenReturn(givenMockResponse);

        Response response = jobResource.handleJsonPost(givenImageName, givenInput, givenPullStrategy, givenUseDockerCache);

        assertThat(response, is(givenMockResponse));
    }

    @Property
    void handleTextGet(
            @ForAll String givenImageName,
            @ForAll("alwaysEmpty") String givenInput,
            @ForAll String givenPullStrategy,
            @ForAll boolean givenUseDockerCache,
            @ForAll @UseType JobRun jobRun,
            @ForAll @UseType JobRunResponse jobRunResponse,
            @ForAll("responseMock") Response givenMockResponse) throws SecretsStoreException {
        when(jobServiceMock.runJob(givenImageName, givenInput, givenPullStrategy, givenUseDockerCache))
                .thenReturn(jobRun);
        when(responseConverterMock.from(jobRun))
                .thenReturn(jobRunResponse);
        when(responseCreatorMock.createResponseFrom(jobRunResponse))
                .thenReturn(givenMockResponse);

        Response response = jobResource.handleTextGet(givenImageName, givenPullStrategy, givenUseDockerCache);

        assertThat(response, is(givenMockResponse));
    }

    @Property
    void handleJsonGet(
            @ForAll String givenImageName,
            @ForAll("alwaysEmpty") String givenInput,
            @ForAll String givenPullStrategy,
            @ForAll boolean givenUseDockerCache,
            @ForAll @UseType JobRun jobRun,
            @ForAll @UseType JobRunResponse jobRunResponse,
            @ForAll("responseMock") Response givenMockResponse) throws SecretsStoreException {
        when(jobServiceMock.runJob(givenImageName, givenInput, givenPullStrategy, givenUseDockerCache))
                .thenReturn(jobRun);
        when(responseConverterMock.from(jobRun))
                .thenReturn(jobRunResponse);
        when(responseCreatorMock.createJsonResponseFrom(jobRunResponse))
                .thenReturn(givenMockResponse);

        Response response = jobResource.handleJsonGet(givenImageName, givenPullStrategy, givenUseDockerCache);

        assertThat(response, is(givenMockResponse));
    }

    @Provide
    Arbitrary<Response> responseMock() {
        return Arbitraries.just(mock(Response.class));
    }

    @Provide
    Arbitrary<String> alwaysEmpty() {
        return Arbitraries.just("");
    }
}
