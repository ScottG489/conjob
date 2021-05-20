package conjob.resource.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.Optional;

public class EveryResponseFilter implements ContainerResponseFilter {
    private static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    private static final String TRACE_ID_NAME = "traceId";
    private final MDCAdapter mdcAdapter;

    public EveryResponseFilter(MDCAdapter mdcAdapter) {
        this.mdcAdapter = mdcAdapter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        addTraceIdHeader(responseContext);
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
    }

    private void addTraceIdHeader(ContainerResponseContext responseContext) {
        Optional.ofNullable(mdcAdapter.get(TRACE_ID_NAME))
                .ifPresent(traceId ->
                        responseContext.getHeaders().add(TRACE_ID_HEADER_NAME, traceId));
    }
}
