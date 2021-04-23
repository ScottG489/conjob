package conjob.resource.admin.task;

import conjob.ConJobConfiguration;
import io.dropwizard.servlets.tasks.Task;
import lombok.Value;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: Make sure to review the usage of all the configurations updated here. Need to make sure that the
// TODO:   values aren't being used in a way where they may cause a race condition or update in the middle
// TODO:   of processing.
public class ConfigTask extends Task {
    private ConJobConfiguration config;

    private final Map<String, ConfigMethods> configFieldMethods = Map.ofEntries(
            Map.entry(
                    "conjob.job.limit.maxGlobalRequestsPerSecond",
                    new ConfigMethods(
                            () -> config.getConjob().getJob().getLimit().getMaxGlobalRequestsPerSecond(),
                            value -> config.getConjob().getJob().getLimit().setMaxGlobalRequestsPerSecond(value))),
            Map.entry(
                    "conjob.job.limit.maxConcurrentRuns",
                    new ConfigMethods(
                            () -> config.getConjob().getJob().getLimit().getMaxConcurrentRuns(),
                            value -> config.getConjob().getJob().getLimit().setMaxConcurrentRuns(value))),
            Map.entry(
                    "conjob.job.limit.maxTimeoutSeconds",
                    new ConfigMethods(
                            () -> config.getConjob().getJob().getLimit().getMaxTimeoutSeconds(),
                            value -> config.getConjob().getJob().getLimit().setMaxTimeoutSeconds(value))),
            Map.entry(
                    "conjob.job.limit.maxKillTimeoutSeconds",
                    new ConfigMethods(
                            () -> config.getConjob().getJob().getLimit().getMaxKillTimeoutSeconds(),
                            value -> config.getConjob().getJob().getLimit().setMaxKillTimeoutSeconds(value)))
    );

    public ConfigTask(ConJobConfiguration config) {
        super("config");
        this.config = config;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        String originalConfig = configFieldMethods.entrySet()
                .stream().map(configEntry ->
                        configEntry.getKey() + "=" + configEntry.getValue().getReadMethod().get() + "&")
                .collect(Collectors.joining());
        originalConfig = originalConfig.substring(0, originalConfig.length() - 1);
        parameters.forEach(this::updateConfig);

        output.write(originalConfig);
    }

    private void updateConfig(String configKey, List<String> configValue) {
        Optional.ofNullable(configValue)
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0))
                .map(Long::valueOf)
                .ifPresent(configFieldMethods.get(configKey).getWriteMethod());
    }

    @Value
    private static class ConfigMethods {
        Supplier<Long> readMethod;
        Consumer<Long> writeMethod;
    }
}
