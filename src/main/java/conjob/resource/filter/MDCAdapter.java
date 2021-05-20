package conjob.resource.filter;

import org.jboss.logging.MDC;

public class MDCAdapter {
    public Object get(String key) {
        return MDC.get(key);
    }

    public void put(String key, String val) {
        MDC.put(key, val);
    }
}
