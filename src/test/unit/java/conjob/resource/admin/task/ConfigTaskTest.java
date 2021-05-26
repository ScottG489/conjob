package conjob.resource.admin.task;

import conjob.ConJobConfiguration;
import conjob.config.*;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConfigTaskTest {
    private ConJobConfiguration conJobConfiguration;
    private ConfigTask configTask;

    @BeforeTry
    void beforeEach() {
        conJobConfiguration = new ConJobConfiguration();
        configTask = new ConfigTask(conJobConfiguration);
    }

    @Label("Given a conjob configuration, " +
            "and new config values, " +
            "when updating the config with new values, " +
            "then the response output should contain the original configuration.")
    void respondWithOriginalConfig(
            @ForAll("conjobConfig") ConJobConfig conjobConfig,
            @ForAll Map<
                    @From("jobLimitConfigKey") String,
                    List<@From("stringLong") String>> parameters) {
        Long originalMaxGlobalRequestsPerSecond = conjobConfig.getJob().getLimit().getMaxGlobalRequestsPerSecond();
        Long originalMaxConcurrentRuns = conjobConfig.getJob().getLimit().getMaxConcurrentRuns();
        Long originalMaxTimeoutSeconds = conjobConfig.getJob().getLimit().getMaxTimeoutSeconds();
        Long originalMaxKillTimeoutSeconds = conjobConfig.getJob().getLimit().getMaxKillTimeoutSeconds();

        conJobConfiguration.setConjob(conjobConfig);
        PrintWriter writerMock = mock(PrintWriter.class);

        configTask.execute(parameters, writerMock);

        verify(writerMock).write(contains("conjob.job.limit.maxGlobalRequestsPerSecond" + "=" + originalMaxGlobalRequestsPerSecond));
        verify(writerMock).write(contains("conjob.job.limit.maxConcurrentRuns" + "=" + originalMaxConcurrentRuns));
        verify(writerMock).write(contains("conjob.job.limit.maxTimeoutSeconds" + "=" + originalMaxTimeoutSeconds));
        verify(writerMock).write(contains("conjob.job.limit.maxKillTimeoutSeconds" + "=" + originalMaxKillTimeoutSeconds));
    }

    @Label("Given a conjob configuration, " +
            "and new config values, " +
            "when updating the config with new values, " +
            "then fields in the config should be updated with new values, " +
            "and fields not updated should be the same as the originals.")
    void updateConfigWithNewValues(
            @ForAll("conjobConfig") ConJobConfig conjobConfig,
            @ForAll Map<
                    @From("jobLimitConfigKey") String,
                    List<@From("stringLong") String>> parameters) {
        String originalMaxGlobalRequestsPerSecond = String.valueOf(conjobConfig.getJob().getLimit().getMaxGlobalRequestsPerSecond());
        String originalMaxConcurrentRuns = String.valueOf(conjobConfig.getJob().getLimit().getMaxConcurrentRuns());
        String originalMaxTimeoutSeconds = String.valueOf(conjobConfig.getJob().getLimit().getMaxTimeoutSeconds());
        String originalMaxKillTimeoutSeconds = String.valueOf(conjobConfig.getJob().getLimit().getMaxKillTimeoutSeconds());

        conJobConfiguration.setConjob(conjobConfig);
        PrintWriter writerMock = mock(PrintWriter.class);

        configTask.execute(parameters, writerMock);

        String maxGlobalRequestsPerSecond = String.valueOf(conjobConfig.getJob().getLimit().getMaxGlobalRequestsPerSecond());
        String maxConcurrentRuns = String.valueOf(conjobConfig.getJob().getLimit().getMaxConcurrentRuns());
        String maxTimeoutSeconds = String.valueOf(conjobConfig.getJob().getLimit().getMaxTimeoutSeconds());
        String maxKillTimeoutSeconds = String.valueOf(conjobConfig.getJob().getLimit().getMaxKillTimeoutSeconds());

        assertThat(maxGlobalRequestsPerSecond, is(newValueOrOriginalIfNoChange(parameters.get("conjob.job.limit.maxGlobalRequestsPerSecond"), originalMaxGlobalRequestsPerSecond)));
        assertThat(maxConcurrentRuns, is(newValueOrOriginalIfNoChange(parameters.get("conjob.job.limit.maxConcurrentRuns"), originalMaxConcurrentRuns)));
        assertThat(maxTimeoutSeconds, is(newValueOrOriginalIfNoChange(parameters.get("conjob.job.limit.maxTimeoutSeconds"), originalMaxTimeoutSeconds)));
        assertThat(maxKillTimeoutSeconds, is(newValueOrOriginalIfNoChange(parameters.get("conjob.job.limit.maxKillTimeoutSeconds"), originalMaxKillTimeoutSeconds)));
    }

    private String newValueOrOriginalIfNoChange(List<String> configKey, String original) {
        return Optional.ofNullable(configKey)
                .filter(not(List::isEmpty))
                .map(this::firstElement)
                .orElse(original);
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