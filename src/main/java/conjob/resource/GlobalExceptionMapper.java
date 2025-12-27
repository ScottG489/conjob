package conjob.resource;

import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.core.Response;

// This exception handler catches all exceptions thrown from our resources. This should only be a fallback and
//   a best effort should be made to be aware of any exceptions that could be thrown and handle them explicitly.
//   Any time this code is run it should be considered a bug and an explicit error handler should be written as a
//   fix.
@Slf4j
public class GlobalExceptionMapper implements jakarta.ws.rs.ext.ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        exception.printStackTrace();
        return Response.serverError().build();
    }
}

