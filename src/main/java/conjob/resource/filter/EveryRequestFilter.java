package conjob.resource.filter;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class EveryRequestFilter implements ContainerRequestFilter {
    static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    static final String TRACE_ID_NAME = "traceId";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestTraceId = requestContext.getHeaderString(TRACE_ID_HEADER_NAME);
        MDC.put(TRACE_ID_NAME,
                Objects.requireNonNullElse(
                        requestTraceId,
                        generateGoodEnoughForNowUUID()));
    }

    private String generateGoodEnoughForNowUUID() {
        return UUID.randomUUID().toString().substring(24);
    }
}
