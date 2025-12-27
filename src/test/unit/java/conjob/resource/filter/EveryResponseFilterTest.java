package conjob.resource.filter;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

import static org.mockito.Mockito.*;

class EveryResponseFilterTest {
    static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    private static final String TRACE_ID_NAME = "traceId";

    @Test
    @Label("Given the filter, " +
            "and no MDC trace id exists, " +
            "when it's filtering a response, " +
            "should add the wildcard allow origin header to the response.")
    void addAllowOriginHeader() {
        MDCAdapter mockMdc = mock(MDCAdapter.class);
        EveryResponseFilter everyResponseFilter = new EveryResponseFilter(mockMdc);
        ContainerResponseContext resp = mock(ContainerResponseContext.class, RETURNS_DEEP_STUBS);
        when(mockMdc.get(TRACE_ID_NAME))
                .thenReturn(null);

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
        MDCAdapter mockMdc = mock(MDCAdapter.class);
        EveryResponseFilter everyResponseFilter = new EveryResponseFilter(mockMdc);
        ContainerResponseContext resp = mock(ContainerResponseContext.class, RETURNS_DEEP_STUBS);
        when(mockMdc.get(TRACE_ID_NAME))
                .thenReturn(givenTraceId);

        everyResponseFilter.filter(mock(ContainerRequestContext.class), resp);

        verify(resp.getHeaders(), times(1)).add(TRACE_ID_HEADER_NAME, givenTraceId);
    }
}
