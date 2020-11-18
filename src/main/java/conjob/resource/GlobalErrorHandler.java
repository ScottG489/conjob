package conjob.resource;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// This error handler catches errors that happen before getting into our resource classes (e.g. param validation).
//   The initial reason it was introduced was to keep from displaying HTML error pages. It makes no sense for an
//   API to be displaying error pages much less returning HTML. This class may be expanded as seen fit.
// NOTE: This is also run after the global exception mapper if it returns a 4xx or 5xx.
public class GlobalErrorHandler extends ErrorPageErrorHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        // Do nothing
    }
}
