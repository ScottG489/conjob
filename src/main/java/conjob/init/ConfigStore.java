package conjob.init;

import conjob.config.ConJobConfig;
import conjob.resource.admin.task.ConjobConfigAccessor;

import java.util.Map;
import java.util.stream.Stream;

public class ConfigStore {
    private final Map<String, ConjobConfigAccessor> configAccessors;

    public ConfigStore(ConJobConfig config) {
        configAccessors = initConfigKeyAccessors(config);
    }

    public Long getByKey(String configKey) {
        return configAccessors.get(configKey).getReadMethod().get();
    }

    public void setByKey(String configKey, Long newValue) {
        configAccessors.get(configKey).getWriteMethod().accept(newValue);
    }

    public Stream<Map.Entry<String, Long>> getAll() {
        return configAccessors.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getReadMethod().get()));
    }

    private Map<String, ConjobConfigAccessor> initConfigKeyAccessors(ConJobConfig config) {
        return Map.ofEntries(
                Map.entry(
                        "conjob.job.limit.maxGlobalRequestsPerSecond",
                        new ConjobConfigAccessor(
                                () -> config.getJob().getLimit().getMaxGlobalRequestsPerSecond(),
                                value -> config.getJob().getLimit().setMaxGlobalRequestsPerSecond(value))),
                Map.entry(
                        "conjob.job.limit.maxConcurrentRuns",
                        new ConjobConfigAccessor(
                                () -> config.getJob().getLimit().getMaxConcurrentRuns(),
                                value -> config.getJob().getLimit().setMaxConcurrentRuns(value))),
                Map.entry(
                        "conjob.job.limit.maxTimeoutSeconds",
                        new ConjobConfigAccessor(
                                () -> config.getJob().getLimit().getMaxTimeoutSeconds(),
                                value -> config.getJob().getLimit().setMaxTimeoutSeconds(value))),
                Map.entry(
                        "conjob.job.limit.maxKillTimeoutSeconds",
                        new ConjobConfigAccessor(
                                () -> config.getJob().getLimit().getMaxKillTimeoutSeconds(),
                                value -> config.getJob().getLimit().setMaxKillTimeoutSeconds(value)))
        );
    }
}