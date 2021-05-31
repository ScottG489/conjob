package conjob.resource.admin.task;

import conjob.config.*;
import conjob.init.ConfigStore;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

class ConfigTaskTest {
    @Property
    @Label("Given a conjob configuration, " +
            "and new config values, " +
            "when updating the config with new values, " +
            "then the response output should contain the original configuration.")
    void respondWithOriginalConfig(
            @ForAll String givenQueryString,
            @ForAll Map<
                    @From("jobLimitConfigKey") String,
                    List<@From("stringLong") String>> parameters) {
        PrintWriter writerMock = mock(PrintWriter.class);

        ConfigMapper configMapper = mock(ConfigMapper.class);
        ConfigStore configStore = mock(ConfigStore.class);
        Stream<Map.Entry<String, Long>> mockStream = Stream.empty();

        when(configStore.getAll()).thenReturn(mockStream);
        when(configMapper.toQueryString(configStore.getAll())).thenReturn(givenQueryString);

        new ConfigTask(configStore, configMapper)
                .execute(parameters, writerMock);

        verify(configMapper, times(1)).toQueryString(mockStream);
        verify(writerMock).write(givenQueryString);
    }

    @Property
    @Label("Given a conjob configuration, " +
            "and new config values, " +
            "when updating the config with new values, " +
            "then fields in the config should be updated with new values, " +
            "and fields not updated should be the same as the originals.")
    void updateConfigWithNewValues(
            @ForAll String givenQueryString,
            @ForAll Map<
                    @From("jobLimitConfigKey") String,
                    @NotEmpty List<@From("stringLong") String>> parameters) {
        PrintWriter writerMock = mock(PrintWriter.class);

        ConfigMapper configMapper = mock(ConfigMapper.class);
        ConfigStore configStore = mock(ConfigStore.class);
        Stream<Map.Entry<String, Long>> mockStream = Stream.empty();

        when(configStore.getAll()).thenReturn(mockStream);
        when(configMapper.toQueryString(configStore.getAll())).thenReturn(givenQueryString);

        new ConfigTask(configStore, configMapper)
                .execute(parameters, writerMock);

        verify(configMapper, times(1)).toQueryString(mockStream);
        parameters.forEach((f, v) -> {
            verify(configStore, times(1)).setByKey(f, Long.valueOf(v.get(0)));
        });
        verify(writerMock).write(givenQueryString);
    }

    String firstElement(List<String> l) {
        return l.get(0);
    }

    @Provide
    Arbitrary<ConJobConfig> conjobConfig() {
        Arbitrary<DockerConfig> dockerConfig = Arbitraries.forType(DockerConfig.class);
        Arbitrary<AdminConfig> adminConfig = Arbitraries.forType(AdminConfig.class);
        Arbitrary<AuthConfig> authConfig = Arbitraries.forType(AuthConfig.class);
        Arbitrary<JobConfig> jobConfig = Arbitraries.forType(JobConfig.LimitConfig.class)
                .map(JobConfig::new);

        return Combinators.combine(dockerConfig, adminConfig, authConfig, jobConfig)
                .as(ConJobConfig::new);
    }

    @Provide
    Arbitrary<String> jobLimitConfigKey() {
        return Arbitraries.of(
                "conjob.job.limit.maxGlobalRequestsPerSecond",
                "conjob.job.limit.maxConcurrentRuns",
                "conjob.job.limit.maxTimeoutSeconds",
                "conjob.job.limit.maxKillTimeoutSeconds");
    }

    @Provide
    Arbitrary<String> stringLong() {
        return Arbitraries.longs().map(String::valueOf);
    }
}