package conjob.resource.admin.task;

import conjob.init.ConfigStore;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO: Make sure to review the usage of all the configurations updated here. Need to make sure that the
// TODO:   values aren't being used in a way where they may cause a race condition or update in the middle
// TODO:   of processing.
public class ConfigTask extends Task {
    private final ConfigStore configStore;

    public ConfigTask(ConfigStore configStore) {
        super("config");
        this.configStore = configStore;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        String originalConfig = configStore.getAll()
                .map(configEntry ->
                        configEntry.getKey() + "=" + configEntry.getValue() + "&")
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
                .ifPresent(value -> configStore.setByKey(configKey, value));
    }
}
