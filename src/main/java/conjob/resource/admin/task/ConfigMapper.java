package conjob.resource.admin.task;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigMapper {
    public String toQueryString(Stream<Map.Entry<String, Long>> allConfigs) {
        return allConfigs
                .map(configEntry ->
                        configEntry.getKey() + "=" + configEntry.getValue())
                .collect(Collectors.joining("&"));
    }
}