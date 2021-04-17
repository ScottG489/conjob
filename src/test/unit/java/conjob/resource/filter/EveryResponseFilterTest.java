package conjob.resource.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import static org.mockito.Mockito.*;

class EveryResponseFilterTest {

    @Test
    @DisplayName("Given the filter, " +
            "when it's filtering a response, " +
            "should add the wildcard allow origin header to the response.")
    void filter() {
        EveryResponseFilter everyResponseFilter = new EveryResponseFilter();
        ContainerResponseContext resp = mock(ContainerResponseContext.class, RETURNS_DEEP_STUBS);
        everyResponseFilter.filter(mock(ContainerRequestContext.class), resp);

        verify(resp.getHeaders()).add("Access-Control-Allow-Origin", "*");
    }
}
