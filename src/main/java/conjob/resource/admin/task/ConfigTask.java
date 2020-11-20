package conjob.resource.admin.task;

import conjob.ConJobConfiguration;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

// TODO: Make sure to review the usage of all the configurations updated here. Need to make sure that the
// TODO:   values aren't being used in a way where they may cause a race condition or update in the middle
// TODO:   of processing.
public class ConfigTask extends Task {
    ConJobConfiguration config;

    Map<String, Consumer<Long>> map = Map.ofEntries(
            Map.entry("conjob.job.limit.maxGlobalRequestsPerSecond",
                    value -> config.getConjob().getJob().getLimit().setMaxGlobalRequestsPerSecond(value)),
            Map.entry("conjob.job.limit.maxConcurrentRuns",
                    value -> config.getConjob().getJob().getLimit().setMaxConcurrentRuns(value)),
            Map.entry("conjob.job.limit.maxTimeoutMinutes",
                    value -> config.getConjob().getJob().getLimit().setMaxTimeoutMinutes(value)),
            Map.entry("conjob.job.limit.maxKillTimeoutSeconds",
                    value -> config.getConjob().getJob().getLimit().setMaxKillTimeoutSeconds(value))
    );

    public ConfigTask(ConJobConfiguration config) {
        super("config");
        this.config = config;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        parameters.forEach(this::updateConfig);
    }

    private void updateConfig(String configKey, List<String> configValue) {
        Optional.ofNullable(configValue)
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0))
                .map(Long::valueOf)
                .ifPresent(map.get(configKey));
    }
}
