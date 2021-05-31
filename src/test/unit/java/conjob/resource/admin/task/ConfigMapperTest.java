package conjob.resource.admin.task;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

class ConfigMapperTest {

    @Property
    @Label("Given a map of config values for config fields, " +
            "when converting them to a query string, " +
            "then the keys and values should appear in the final query string.")
    void toQueryString(@ForAll Map<String, Long> configKeyValues) {
        String queryString = new ConfigMapper().toQueryString(configKeyValues.entrySet().stream());

        configKeyValues.forEach((key, value) ->
                assertThat(queryString, containsString(key + "=" + value)));
    }
}