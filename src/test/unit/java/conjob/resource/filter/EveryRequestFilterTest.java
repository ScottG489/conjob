package conjob.resource.filter;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EveryRequestFilterTest {
    static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    static final String TRACE_ID_NAME = "traceId";

    @Test
    @DisplayName("Given a request filter, " +
            "and a request that doesn't have a trace id header, " +
            "when the request is filtered, " +
            "should register a new trace id with the MDC.")
    void addNewTraceIdToMDC() throws IOException {
        EveryRequestFilter everyRequestFilter = new EveryRequestFilter();
        ContainerRequestContext mockRequest = mock(ContainerRequestContext.class);
        when(mockRequest.getHeaderString(TRACE_ID_HEADER_NAME)).thenReturn(null);

        everyRequestFilter.filter(mockRequest);

        assertThat(MDC.get(TRACE_ID_NAME), matchesRegex("[0-9a-f]{12}"));
    }

    @Property
    @Label("Given a request filter, " +
            "and a request that has a trace id header, " +
            "when the request is filtered, " +
            "should register the request's trace id with the MDC.")
    void addTraceIdOnHeaderToMDC(@ForAll String givenTraceId) throws IOException {
        EveryRequestFilter everyRequestFilter = new EveryRequestFilter();
        ContainerRequestContext mockRequest = mock(ContainerRequestContext.class);
        when(mockRequest.getHeaderString(TRACE_ID_HEADER_NAME)).thenReturn(givenTraceId);

        everyRequestFilter.filter(mockRequest);

        assertThat(MDC.get(TRACE_ID_NAME), is(givenTraceId));
    }
}
