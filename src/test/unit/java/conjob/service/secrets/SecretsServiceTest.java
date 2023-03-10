package conjob.service.secrets;

import conjob.core.job.config.ConfigUtil;
import conjob.core.secrets.SecretsContainerCreator;
import conjob.core.secrets.SecretsDockerAdapter;
import conjob.core.secrets.TempSecretsFileUtil;
import conjob.core.secrets.model.SecretsConfig;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.*;

class SecretsServiceTest {
    private static final String INTERMEDIARY_CONTAINER_IMAGE = "tianon/true:latest";
    String CONTAINER_NAME_PREFIX = "temp-container-";
    String CONTAINER_DESTINATION_PATH = "/temp";

    private SecretsService secretsService;
    private SecretsDockerAdapter mockSecretsAdapter;
    private SecretsContainerCreator mockSecretsContainerCreator;
    private TempSecretsFileUtil mockSecretsFileUtil;
    private UniqueContainerNameGenerator mockNameGenerator;
    private ConfigUtil mockConfigUtil;

    @BeforeTry
    void beforeEach() {
        mockSecretsAdapter = mock(SecretsDockerAdapter.class);
        mockSecretsContainerCreator = mock(SecretsContainerCreator.class);
        mockSecretsFileUtil = mock(TempSecretsFileUtil.class, RETURNS_DEEP_STUBS);
        mockNameGenerator = mock(UniqueContainerNameGenerator.class);
        mockConfigUtil = mock(ConfigUtil.class);
        secretsService = new SecretsService(
                mockSecretsAdapter,
                mockSecretsContainerCreator,
                mockSecretsFileUtil,
                mockNameGenerator,
                mockConfigUtil);
    }

    @Property
    @Label("Given an image name, " +
            "and secrets, " +
            "when the secrets are associated with the image, " +
            "should be successful.")
    void jobNotFound(
            @ForAll String imageName,
            @ForAll String secrets,
            @ForAll String givenSecretsVolumeName,
            @ForAll String givenContainerName,
            @ForAll String givenContainerId,
            @ForAll("secretsDir") Path givenSecretsDir
    ) throws IOException {
        SecretsConfig secretsConfig = new SecretsConfig(
                givenSecretsVolumeName,
                CONTAINER_DESTINATION_PATH,
                INTERMEDIARY_CONTAINER_IMAGE,
                givenContainerName);

        when(mockConfigUtil.translateToSecretsVolumeName(imageName)).thenReturn(givenSecretsVolumeName);
        when(mockNameGenerator.generate(CONTAINER_NAME_PREFIX)).thenReturn(givenContainerName);
        when(mockSecretsContainerCreator.createIntermediaryContainer(secretsConfig))
                .thenReturn(givenContainerId);
        when(mockSecretsFileUtil.createSecretsFile(secrets).getParentFile().toPath())
                .thenReturn(givenSecretsDir);

        secretsService.createsSecret(imageName, secrets);

        verify(mockSecretsAdapter, times(1))
                .copySecretsToVolume(givenSecretsDir, givenContainerId, CONTAINER_DESTINATION_PATH);
        verify(mockSecretsFileUtil, times(1)).delete(givenSecretsDir);
        verify(mockSecretsAdapter, times(1)).removeContainer(givenContainerId);
    }

    @Provide
    Arbitrary<Path> secretsDir() {
        return Arbitraries.strings()
                .alpha().numeric()
                .map(Path::of);
    }
}
