package conjob.resource.filter;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EveryRequestFilterTest {
    static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    static final String TRACE_ID_NAME = "traceId";

    @Test
    @DisplayName("Given a request filter, " +
            "and a request that doesn't have a trace id header, " +
            "when the request is filtered, " +
            "should register a new trace id with the MDC.")
    void addNewTraceIdToMDC() throws IOException {
        MDCAdapter mockMdc = mock(MDCAdapter.class);
        EveryRequestFilter everyRequestFilter = new EveryRequestFilter(mockMdc);
        ContainerRequestContext mockRequest = mock(ContainerRequestContext.class);
        when(mockRequest.getHeaderString(TRACE_ID_HEADER_NAME)).thenReturn(null);

        everyRequestFilter.filter(mockRequest);

        verify(mockMdc, times(1))
                .put(eq(TRACE_ID_NAME), matches("^[0-9a-f]{12}$"));
    }

    @Property
    @Label("Given a request filter, " +
            "and a request that has a trace id header, " +
            "when the request is filtered, " +
            "should register the request's trace id with the MDC.")
    void addTraceIdOnHeaderToMDC(@ForAll String givenTraceId) throws IOException {
        MDCAdapter mockMdc = mock(MDCAdapter.class);
        EveryRequestFilter everyRequestFilter = new EveryRequestFilter(mockMdc);
        ContainerRequestContext mockRequest = mock(ContainerRequestContext.class);
        when(mockRequest.getHeaderString(TRACE_ID_HEADER_NAME)).thenReturn(givenTraceId);

        everyRequestFilter.filter(mockRequest);

        verify(mockMdc, times(1)).put(TRACE_ID_NAME, givenTraceId);
    }
}
