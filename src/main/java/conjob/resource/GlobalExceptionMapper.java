package conjob.resource;

import javax.ws.rs.core.Response;

// This exception handler catches all exceptions thrown from our resources. This should only be a fallback and
//   a best effort should be made to be aware of any exceptions that could be thrown and handle them explicitly.
//   Any time this code is run it should be considered a bug and an explicit error handler should be written as a
//   fix.
public class GlobalExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        return Response.serverError().build();
    }
}

