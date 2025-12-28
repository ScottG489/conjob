package conjob.init;

import conjob.config.*;
import net.jqwik.api.*;

import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ConfigStoreTest {
    @Property
    @Label("Given a conjob config, " +
            "when initializing a config store with it, " +
            "and retrieving the config values by key, " +
            "then the retrieved values should be the same as the original config's values.")
    void getOriginalValues(@ForAll("conjobConfig") ConJobConfig config) {
        Long maxConcurrentRuns = config.getJob().getLimit().getMaxConcurrentRuns();
        Long maxGlobalRequestsPerSecond = config.getJob().getLimit().getMaxGlobalRequestsPerSecond();
        Long maxKillTimeoutSeconds = config.getJob().getLimit().getMaxKillTimeoutSeconds();
        Long maxTimeoutSeconds = config.getJob().getLimit().getMaxTimeoutSeconds();

        ConfigStore configStore = new ConfigStore(config);

        assertThat(maxConcurrentRuns, is(configStore.getByKey("conjob.job.limit.maxConcurrentRuns")));
        assertThat(maxGlobalRequestsPerSecond, is(configStore.getByKey("conjob.job.limit.maxGlobalRequestsPerSecond")));
        assertThat(maxKillTimeoutSeconds, is(configStore.getByKey("conjob.job.limit.maxKillTimeoutSeconds")));
        assertThat(maxTimeoutSeconds, is(configStore.getByKey("conjob.job.limit.maxTimeoutSeconds")));
    }

    @Property
    @Label("Given a conjob config, " +
            "and new values for all config fields, " +
            "when setting all config fields to the new values, " +
            "then the new values should be the same as the config's values.")
    void setNewValues(
            @ForAll("conjobConfig") ConJobConfig config,
            @ForAll Long newMaxConcurrentRuns,
            @ForAll Long newMaxGlobalRequestsPerSecond,
            @ForAll Long newMaxKillTimeoutSeconds,
            @ForAll Long newMaxTimeoutSeconds) {
        ConfigStore configStore = new ConfigStore(config);
        configStore.setByKey("conjob.job.limit.maxConcurrentRuns", newMaxConcurrentRuns);
        configStore.setByKey("conjob.job.limit.maxGlobalRequestsPerSecond", newMaxGlobalRequestsPerSecond);
        configStore.setByKey("conjob.job.limit.maxKillTimeoutSeconds", newMaxKillTimeoutSeconds);
        configStore.setByKey("conjob.job.limit.maxTimeoutSeconds", newMaxTimeoutSeconds);

        Long maxConcurrentRuns = config.getJob().getLimit().getMaxConcurrentRuns();
        Long maxGlobalRequestsPerSecond = config.getJob().getLimit().getMaxGlobalRequestsPerSecond();
        Long maxKillTimeoutSeconds = config.getJob().getLimit().getMaxKillTimeoutSeconds();
        Long maxTimeoutSeconds = config.getJob().getLimit().getMaxTimeoutSeconds();

        assertThat(maxConcurrentRuns, is(newMaxConcurrentRuns));
        assertThat(maxGlobalRequestsPerSecond, is(newMaxGlobalRequestsPerSecond));
        assertThat(maxKillTimeoutSeconds, is(newMaxKillTimeoutSeconds));
        assertThat(maxTimeoutSeconds, is(newMaxTimeoutSeconds));
    }

    @Property
    @Label("Given a conjob config, " +
            "when getting all config store entries, " +
            "then they should contain all config field keys.")
    void getAll(
            @ForAll("conjobConfig") ConJobConfig config) {
        Map<String, Long> configKeyValues = new ConfigStore(config).getAll()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(configKeyValues.containsKey("conjob.job.limit.maxConcurrentRuns"), is(true));
        assertThat(configKeyValues.containsKey("conjob.job.limit.maxGlobalRequestsPerSecond"), is(true));
        assertThat(configKeyValues.containsKey("conjob.job.limit.maxKillTimeoutSeconds"), is(true));
        assertThat(configKeyValues.containsKey("conjob.job.limit.maxTimeoutSeconds"), is(true));
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
}
