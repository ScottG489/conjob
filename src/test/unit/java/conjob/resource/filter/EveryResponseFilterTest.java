package conjob.resource.filter;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import static org.mockito.Mockito.*;

class EveryResponseFilterTest {
    static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    private static final String TRACE_ID_NAME = "traceId";

    @Test
    @DisplayName("Given the filter, " +
            "and no MDC trace id exists, " +
            "when it's filtering a response, " +
            "should add the wildcard allow origin header to the response.")
    void addAllowOriginHeader() {
        EveryResponseFilter everyResponseFilter = new EveryResponseFilter();
        ContainerResponseContext resp = mock(ContainerResponseContext.class, RETURNS_DEEP_STUBS);

        everyResponseFilter.filter(mock(ContainerRequestContext.class), resp);

        verify(resp.getHeaders(), times(1)).add("Access-Control-Allow-Origin", "*");
        verify(resp.getHeaders(), times(0)).add(eq(TRACE_ID_HEADER_NAME), any());
    }

    @Property
    @Label("Given the filter, " +
            "and a MDC trace id exists, " +
            "when it's filtering a response, " +
            "should add the trace id header to the response.")
    void addTraceIdIfPresent(@ForAll String givenTraceId) {
        EveryResponseFilter everyResponseFilter = new EveryResponseFilter();
        ContainerResponseContext resp = mock(ContainerResponseContext.class, RETURNS_DEEP_STUBS);

        MDC.put(TRACE_ID_NAME, givenTraceId);
        everyResponseFilter.filter(mock(ContainerRequestContext.class), resp);

        verify(resp.getHeaders(), times(1)).add(eq(TRACE_ID_HEADER_NAME), any());
        MDC.clear();
    }
}
