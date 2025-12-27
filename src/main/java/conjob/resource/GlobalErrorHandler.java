package conjob.resource;

import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

// This error handler catches errors that happen before getting into our resource classes (e.g. param validation).
//   The initial reason it was introduced was to keep from displaying HTML error pages. It makes no sense for an
//   API to be displaying error pages much less returning HTML. This class may be expanded as seen fit.
// NOTE: This is also run after the global exception mapper if it returns a 4xx or 5xx.
public class GlobalErrorHandler extends ErrorPageErrorHandler {
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        // Do nothing - just succeed the callback to prevent HTML error pages
        callback.succeeded();
        return true;
    }
}
